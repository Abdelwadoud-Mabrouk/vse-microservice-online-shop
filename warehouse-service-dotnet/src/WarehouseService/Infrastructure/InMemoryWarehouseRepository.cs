using System.Collections.Concurrent;
using WarehouseService.Application;
using WarehouseService.Domain;

namespace WarehouseService.Infrastructure;

public sealed class InMemoryWarehouseRepository : IWarehouseRepository
{
    private readonly object _sync = new();
    private readonly Dictionary<Guid, StockItem> _availableStock;
    private readonly ConcurrentDictionary<Guid, Reservation> _reservations = new();

    public InMemoryWarehouseRepository(IConfiguration configuration)
    {
        _availableStock = LoadInitialStock(configuration["WAREHOUSE_INITIAL_STOCK"]);
    }

    public IReadOnlyList<InventoryItemDto> GetInventory()
    {
        lock (_sync)
        {
            return _availableStock.Values
                .OrderBy(item => item.Sku, StringComparer.OrdinalIgnoreCase)
                .Select(item => new InventoryItemDto(item.ProductId, item.Sku, item.Name, item.Category, item.AvailableQuantity))
                .ToArray();
        }
    }

    public InventoryItemDto? FindInventoryItem(Guid productId)
    {
        lock (_sync)
        {
            return _availableStock.TryGetValue(productId, out var item)
                ? new InventoryItemDto(item.ProductId, item.Sku, item.Name, item.Category, item.AvailableQuantity)
                : null;
        }
    }

    public InventoryChangeResult AddInventoryItem(SaveInventoryItemCommand command)
    {
        lock (_sync)
        {
            if (_availableStock.ContainsKey(command.ProductId) || _availableStock.Values.Any(item => item.Sku == command.Sku))
            {
                return new InventoryChangeResult(false, "Product id or SKU already exists.", null);
            }

            var item = new StockItem(command.ProductId, command.Sku, command.Name, command.Category, command.AvailableQuantity);
            _availableStock[command.ProductId] = item;
            return new InventoryChangeResult(true, "Product created.", ToDto(item));
        }
    }

    public InventoryChangeResult UpdateInventoryItem(SaveInventoryItemCommand command)
    {
        lock (_sync)
        {
            if (!_availableStock.ContainsKey(command.ProductId))
            {
                return new InventoryChangeResult(false, "Product does not exist.", null);
            }

            if (_availableStock.Values.Any(item => item.ProductId != command.ProductId && item.Sku == command.Sku))
            {
                return new InventoryChangeResult(false, "SKU is already used by another product.", null);
            }

            var item = new StockItem(command.ProductId, command.Sku, command.Name, command.Category, command.AvailableQuantity);
            _availableStock[command.ProductId] = item;
            return new InventoryChangeResult(true, "Product updated.", ToDto(item));
        }
    }

    public InventoryChangeResult SetStockQuantity(SetStockQuantityCommand command)
    {
        lock (_sync)
        {
            if (!_availableStock.TryGetValue(command.ProductId, out var item))
            {
                return new InventoryChangeResult(false, "Product does not exist.", null);
            }

            var changed = item with { AvailableQuantity = command.AvailableQuantity };
            _availableStock[command.ProductId] = changed;
            return new InventoryChangeResult(true, "Stock quantity changed.", ToDto(changed));
        }
    }

    public InventoryChangeResult RemoveInventoryItem(Guid productId)
    {
        lock (_sync)
        {
            if (!_availableStock.Remove(productId, out var item))
            {
                return new InventoryChangeResult(false, "Product does not exist.", null);
            }

            return new InventoryChangeResult(true, "Product removed.", ToDto(item));
        }
    }

    public ReservationResult TryReserve(ReserveStockCommand command)
    {
        lock (_sync)
        {
            var existing = _reservations.Values.SingleOrDefault(value => value.OrderId == command.OrderId);
            if (existing is not null)
            {
                var identical = SameItems(existing.Items, command.Items);
                return identical
                    ? new ReservationResult(true, "Existing reservation returned.", existing.ToDto(), IsReplay: true)
                    : new ReservationResult(false, "orderId is already used by a different reservation request.", existing.ToDto(), IsConflict: true);
            }

            foreach (var item in command.Items)
            {
                if (!_availableStock.TryGetValue(item.ProductId, out var stockItem) || stockItem.AvailableQuantity < item.Quantity)
                {
                    return new ReservationResult(false, $"Product '{item.ProductId}' has insufficient stock.", null);
                }
            }

            foreach (var item in command.Items)
            {
                var stockItem = _availableStock[item.ProductId];
                _availableStock[item.ProductId] = stockItem with
                {
                    AvailableQuantity = stockItem.AvailableQuantity - item.Quantity
                };
            }

            var reservation = Reservation.Create(command.OrderId, command.CorrelationId, command.Items);
            _reservations[reservation.ReservationId] = reservation;

            return new ReservationResult(true, "Reservation created.", reservation.ToDto());
        }
    }

    public ReservationTransitionResult Cancel(Guid reservationId)
    {
        lock (_sync)
        {
            if (!_reservations.TryGetValue(reservationId, out var reservation))
            {
                return new(ReservationTransitionOutcome.NotFound, "Reservation does not exist.", null);
            }

            if (reservation.Status == ReservationStatus.Cancelled)
            {
                return new(ReservationTransitionOutcome.AlreadyApplied, "Reservation was already cancelled.", reservation.ToDto());
            }

            if (reservation.Status != ReservationStatus.Reserved)
            {
                return new(ReservationTransitionOutcome.Conflict, $"Reservation in state {reservation.Status} cannot be cancelled.", reservation.ToDto());
            }

            foreach (var item in reservation.Items)
            {
                if (_availableStock.TryGetValue(item.ProductId, out var stockItem))
                {
                    _availableStock[item.ProductId] = stockItem with
                    {
                        AvailableQuantity = stockItem.AvailableQuantity + item.Quantity
                    };
                }
            }

            var cancelled = reservation.Cancel();
            _reservations[reservationId] = cancelled;
            return new(ReservationTransitionOutcome.Applied, "Reservation cancelled.", cancelled.ToDto());
        }
    }

    public ReservationTransitionResult Commit(Guid reservationId)
    {
        lock (_sync)
        {
            if (!_reservations.TryGetValue(reservationId, out var reservation))
            {
                return new(ReservationTransitionOutcome.NotFound, "Reservation does not exist.", null);
            }

            if (reservation.Status == ReservationStatus.Committed)
            {
                return new(ReservationTransitionOutcome.AlreadyApplied, "Reservation was already committed.", reservation.ToDto());
            }

            if (reservation.Status != ReservationStatus.Reserved)
            {
                return new(ReservationTransitionOutcome.Conflict, $"Reservation in state {reservation.Status} cannot be committed.", reservation.ToDto());
            }

            var committed = reservation.Commit();
            _reservations[reservationId] = committed;
            return new(ReservationTransitionOutcome.Applied, "Reservation committed.", committed.ToDto());
        }
    }

    private static bool SameItems(IReadOnlyList<ReservationItem> left, IReadOnlyList<ReservationItem> right) =>
        left.OrderBy(item => item.ProductId).ThenBy(item => item.Sku)
            .SequenceEqual(right.OrderBy(item => item.ProductId).ThenBy(item => item.Sku));

    private static Dictionary<Guid, StockItem> LoadInitialStock(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return new[]
            {
                new StockItem(Guid.Parse("11111111-1111-4111-8111-111111111111"), "SKU-CHAIR", "Esszimmerstuhl Eiche", "Stuehle", 10),
                new StockItem(Guid.Parse("22222222-2222-4222-8222-222222222222"), "SKU-DESK", "Schreibtisch Oslo", "Tische", 5),
                new StockItem(Guid.Parse("33333333-3333-4333-8333-333333333333"), "SKU-LAMP", "Stehlampe Luna", "Lampen", 20)
            }.ToDictionary(item => item.ProductId);
        }

        return value.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(part => part.Split(':', StringSplitOptions.TrimEntries))
            .Where(parts => parts.Length == 3 && Guid.TryParse(parts[0], out _) && int.TryParse(parts[2], out _))
            .Select(parts => new StockItem(Guid.Parse(parts[0]), parts[1], parts[1], "Moebel", int.Parse(parts[2])))
            .ToDictionary(item => item.ProductId);
    }

    private static InventoryItemDto ToDto(StockItem item)
    {
        return new InventoryItemDto(item.ProductId, item.Sku, item.Name, item.Category, item.AvailableQuantity);
    }

    private sealed record StockItem(Guid ProductId, string Sku, string Name, string Category, int AvailableQuantity);
}
