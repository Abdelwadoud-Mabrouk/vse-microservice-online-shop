using System.Data;
using Microsoft.EntityFrameworkCore;
using WarehouseService.Application;
using WarehouseService.Domain;
using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Repositories;

public sealed class EfWarehouseRepository(
    WarehouseDbContext dbContext,
    ILogger<EfWarehouseRepository> logger) : IWarehouseRepository
{
    public IReadOnlyList<InventoryItemDto> GetInventory()
    {
        return dbContext.Products
            .AsNoTracking()
            .OrderBy(product => product.Category)
            .ThenBy(product => product.Name)
            .Select(product => new InventoryItemDto(
                product.ProductId,
                product.Sku,
                product.Name,
                product.Category,
                product.AvailableQuantity))
            .ToArray();
    }

    public InventoryItemDto? FindInventoryItem(Guid productId)
    {
        return dbContext.Products
            .AsNoTracking()
            .Where(product => product.ProductId == productId)
            .Select(product => ToDto(product))
            .SingleOrDefault();
    }

    public InventoryChangeResult AddInventoryItem(SaveInventoryItemCommand command)
    {
        if (dbContext.Products.Any(product => product.ProductId == command.ProductId || product.Sku == command.Sku))
        {
            return new InventoryChangeResult(false, "Product id or SKU already exists.", null);
        }

        var product = new WarehouseProductEntity
        {
            ProductId = command.ProductId,
            Sku = command.Sku,
            Name = command.Name,
            Category = command.Category,
            AvailableQuantity = command.AvailableQuantity
        };

        dbContext.Products.Add(product);
        dbContext.SaveChanges();

        logger.LogInformation(
            "Created warehouse product {ProductId} with SKU {Sku} and quantity {AvailableQuantity}",
            product.ProductId,
            product.Sku,
            product.AvailableQuantity);

        return new InventoryChangeResult(true, "Product created.", ToDto(product));
    }

    public InventoryChangeResult UpdateInventoryItem(SaveInventoryItemCommand command)
    {
        var product = dbContext.Products.SingleOrDefault(product => product.ProductId == command.ProductId);
        if (product is null)
        {
            return new InventoryChangeResult(false, "Product does not exist.", null);
        }

        var skuAlreadyUsed = dbContext.Products.Any(product =>
            product.ProductId != command.ProductId && product.Sku == command.Sku);

        if (skuAlreadyUsed)
        {
            return new InventoryChangeResult(false, "SKU is already used by another product.", null);
        }

        product.Sku = command.Sku;
        product.Name = command.Name;
        product.Category = command.Category;
        product.AvailableQuantity = command.AvailableQuantity;

        dbContext.SaveChanges();

        logger.LogInformation(
            "Updated warehouse product {ProductId} with SKU {Sku}",
            product.ProductId,
            product.Sku);

        return new InventoryChangeResult(true, "Product updated.", ToDto(product));
    }

    public InventoryChangeResult SetStockQuantity(SetStockQuantityCommand command)
    {
        var product = dbContext.Products.SingleOrDefault(product => product.ProductId == command.ProductId);
        if (product is null)
        {
            return new InventoryChangeResult(false, "Product does not exist.", null);
        }

        product.AvailableQuantity = command.AvailableQuantity;
        dbContext.SaveChanges();

        logger.LogInformation(
            "Set stock for product {ProductId} to {AvailableQuantity}",
            product.ProductId,
            product.AvailableQuantity);

        return new InventoryChangeResult(true, "Stock quantity changed.", ToDto(product));
    }

    public InventoryChangeResult RemoveInventoryItem(Guid productId)
    {
        var product = dbContext.Products.SingleOrDefault(product => product.ProductId == productId);
        if (product is null)
        {
            return new InventoryChangeResult(false, "Product does not exist.", null);
        }

        var hasOpenReservation = dbContext.ReservationItems.Any(item =>
            item.ProductId == productId &&
            item.Reservation != null &&
            item.Reservation.Status == ReservationStatus.Reserved);

        if (hasOpenReservation)
        {
            return new InventoryChangeResult(false, "Product has an open reservation and cannot be removed.", ToDto(product));
        }

        var dto = ToDto(product);
        dbContext.Products.Remove(product);
        dbContext.SaveChanges();

        logger.LogInformation("Removed warehouse product {ProductId}", productId);

        return new InventoryChangeResult(true, "Product removed.", dto);
    }

    public ReservationResult TryReserve(ReserveStockCommand command)
    {
        using var transaction = dbContext.Database.BeginTransaction(IsolationLevel.ReadCommitted);

        AcquireTransactionLock($"order:{command.OrderId}");
        foreach (var productId in command.Items.Select(item => item.ProductId).Distinct().Order())
        {
            AcquireTransactionLock($"product:{productId}");
        }

        var existingEntity = dbContext.Reservations
            .Include(reservation => reservation.Items)
            .SingleOrDefault(reservation => reservation.OrderId == command.OrderId);
        if (existingEntity is not null)
        {
            var existing = ToDomain(existingEntity);
            var identical = SameItems(existing.Items, command.Items);
            transaction.Rollback();
            return identical
                ? new ReservationResult(true, "Existing reservation returned.", existing.ToDto(), IsReplay: true)
                : new ReservationResult(false, "orderId is already used by a different reservation request.", existing.ToDto(), IsConflict: true);
        }

        foreach (var item in command.Items)
        {
            var product = dbContext.Products.SingleOrDefault(product => product.ProductId == item.ProductId);
            if (product is null)
            {
                transaction.Rollback();
                return new ReservationResult(false, $"Product '{item.ProductId}' does not exist.", null);
            }

            if (product.AvailableQuantity < item.Quantity)
            {
                transaction.Rollback();
                return new ReservationResult(false, $"Product '{item.Sku}' has insufficient stock.", null);
            }
        }

        foreach (var item in command.Items)
        {
            var product = dbContext.Products.Single(product => product.ProductId == item.ProductId);
            product.AvailableQuantity -= item.Quantity;
        }

        var reservation = Reservation.Create(command.OrderId, command.CorrelationId, command.Items);
        dbContext.Reservations.Add(ToEntity(reservation));

        try
        {
            dbContext.SaveChanges();
            transaction.Commit();
        }
        catch (DbUpdateException)
        {
            transaction.Rollback();
            dbContext.ChangeTracker.Clear();

            var concurrentEntity = dbContext.Reservations
                .AsNoTracking()
                .Include(value => value.Items)
                .SingleOrDefault(value => value.OrderId == command.OrderId);
            if (concurrentEntity is null)
            {
                throw;
            }

            var concurrentReservation = ToDomain(concurrentEntity);
            var identical = SameItems(concurrentReservation.Items, command.Items);
            logger.LogInformation(
                "Concurrent reservation retry detected for order {OrderId}; identicalRequest {IdenticalRequest}",
                command.OrderId,
                identical);
            return identical
                ? new ReservationResult(true, "Existing reservation returned.", concurrentReservation.ToDto(), IsReplay: true)
                : new ReservationResult(false, "orderId is already used by a different reservation request.", concurrentReservation.ToDto(), IsConflict: true);
        }

        logger.LogInformation(
            "Reserved stock for order {OrderId} with reservation {ReservationId} and correlation {CorrelationId}",
            reservation.OrderId,
            reservation.ReservationId,
            reservation.CorrelationId);

        return new ReservationResult(true, "Reservation created.", reservation.ToDto());
    }

    public ReservationTransitionResult Cancel(Guid reservationId)
    {
        return ChangeReservationStatus(reservationId, ReservationStatus.Cancelled, returnStock: true);
    }

    public ReservationTransitionResult Commit(Guid reservationId)
    {
        return ChangeReservationStatus(reservationId, ReservationStatus.Committed, returnStock: false);
    }

    private ReservationTransitionResult ChangeReservationStatus(Guid reservationId, ReservationStatus nextStatus, bool returnStock)
    {
        using var transaction = dbContext.Database.BeginTransaction(IsolationLevel.ReadCommitted);
        AcquireTransactionLock($"reservation:{reservationId}");

        var entity = dbContext.Reservations
            .Include(reservation => reservation.Items)
            .SingleOrDefault(reservation => reservation.ReservationId == reservationId);

        if (entity is null)
        {
            transaction.Rollback();
            return new(ReservationTransitionOutcome.NotFound, "Reservation does not exist.", null);
        }

        var reservation = ToDomain(entity);
        if (reservation.Status == nextStatus)
        {
            transaction.Rollback();
            return new(ReservationTransitionOutcome.AlreadyApplied, $"Reservation was already {nextStatus}.", reservation.ToDto());
        }

        if (reservation.Status != ReservationStatus.Reserved)
        {
            transaction.Rollback();
            return new(ReservationTransitionOutcome.Conflict, $"Reservation in state {reservation.Status} cannot transition to {nextStatus}.", reservation.ToDto());
        }

        if (returnStock)
        {
            foreach (var item in reservation.Items)
            {
                var product = dbContext.Products.SingleOrDefault(product => product.ProductId == item.ProductId);
                if (product is not null)
                {
                    product.AvailableQuantity += item.Quantity;
                }
            }
        }

        var changed = nextStatus == ReservationStatus.Cancelled
            ? reservation.Cancel()
            : reservation.Commit();

        entity.Status = changed.Status;
        entity.UpdatedAtUtc = changed.UpdatedAt;

        dbContext.SaveChanges();
        transaction.Commit();

        logger.LogInformation(
            "Changed reservation {ReservationId} to {ReservationStatus}",
            changed.ReservationId,
            changed.Status);

        return new(ReservationTransitionOutcome.Applied, $"Reservation changed to {nextStatus}.", changed.ToDto());
    }

    private void AcquireTransactionLock(string key)
    {
        dbContext.Database.ExecuteSqlInterpolated(
            $"SELECT pg_advisory_xact_lock(hashtextextended({key}, 0))");
    }

    private static bool SameItems(IReadOnlyList<ReservationItem> left, IReadOnlyList<ReservationItem> right) =>
        left.OrderBy(item => item.ProductId).ThenBy(item => item.Sku)
            .SequenceEqual(right.OrderBy(item => item.ProductId).ThenBy(item => item.Sku));

    private static WarehouseReservationEntity ToEntity(Reservation reservation)
    {
        return new WarehouseReservationEntity
        {
            ReservationId = reservation.ReservationId,
            OrderId = reservation.OrderId,
            CorrelationId = reservation.CorrelationId,
            Status = reservation.Status,
            CreatedAtUtc = reservation.CreatedAt,
            UpdatedAtUtc = reservation.UpdatedAt,
            Items = reservation.Items.Select(item => new WarehouseReservationItemEntity
            {
                ReservationId = reservation.ReservationId,
                ProductId = item.ProductId,
                Sku = item.Sku,
                Quantity = item.Quantity
            }).ToList()
        };
    }

    private static InventoryItemDto ToDto(WarehouseProductEntity product)
    {
        return new InventoryItemDto(
            product.ProductId,
            product.Sku,
            product.Name,
            product.Category,
            product.AvailableQuantity);
    }

    private static Reservation ToDomain(WarehouseReservationEntity entity)
    {
        var items = entity.Items
            .OrderBy(item => item.Sku)
            .Select(item => ReservationItem.Create(item.ProductId, item.Sku, item.Quantity))
            .ToArray();

        return new Reservation(
            entity.ReservationId,
            entity.OrderId,
            entity.CorrelationId,
            items,
            entity.Status,
            entity.CreatedAtUtc,
            entity.UpdatedAtUtc);
    }
}
