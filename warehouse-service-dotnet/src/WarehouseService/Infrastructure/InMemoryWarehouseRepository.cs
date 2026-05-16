using System.Collections.Concurrent;
using WarehouseService.Application;
using WarehouseService.Domain;

namespace WarehouseService.Infrastructure;

public sealed class InMemoryWarehouseRepository : IWarehouseRepository
{
    private readonly object _sync = new();
    private readonly Dictionary<string, int> _availableStock;
    private readonly ConcurrentDictionary<Guid, Reservation> _reservations = new();

    public InMemoryWarehouseRepository(IConfiguration configuration)
    {
        _availableStock = LoadInitialStock(configuration["WAREHOUSE_INITIAL_STOCK"]);
    }

    public IReadOnlyDictionary<string, int> GetInventory()
    {
        lock (_sync)
        {
            return _availableStock.ToDictionary(pair => pair.Key, pair => pair.Value);
        }
    }

    public ReservationResult TryReserve(ReserveStockCommand command)
    {
        lock (_sync)
        {
            foreach (var item in command.Items)
            {
                if (!_availableStock.TryGetValue(item.Sku, out var available) || available < item.Quantity)
                {
                    return new ReservationResult(false, $"SKU '{item.Sku}' has insufficient stock.", null);
                }
            }

            foreach (var item in command.Items)
            {
                _availableStock[item.Sku] -= item.Quantity;
            }

            var reservation = Reservation.Create(command.OrderId, command.CorrelationId, command.Items);
            _reservations[reservation.ReservationId] = reservation;

            return new ReservationResult(true, "Reservation created.", reservation.ToDto());
        }
    }

    public Reservation? Cancel(Guid reservationId)
    {
        lock (_sync)
        {
            if (!_reservations.TryGetValue(reservationId, out var reservation) || reservation.Status != ReservationStatus.Reserved)
            {
                return null;
            }

            foreach (var item in reservation.Items)
            {
                _availableStock[item.Sku] = _availableStock.GetValueOrDefault(item.Sku) + item.Quantity;
            }

            var cancelled = reservation.Cancel();
            _reservations[reservationId] = cancelled;
            return cancelled;
        }
    }

    public Reservation? Commit(Guid reservationId)
    {
        lock (_sync)
        {
            if (!_reservations.TryGetValue(reservationId, out var reservation) || reservation.Status != ReservationStatus.Reserved)
            {
                return null;
            }

            var committed = reservation.Commit();
            _reservations[reservationId] = committed;
            return committed;
        }
    }

    private static Dictionary<string, int> LoadInitialStock(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase)
            {
                ["SKU-CHAIR"] = 10,
                ["SKU-DESK"] = 5,
                ["SKU-LAMP"] = 20
            };
        }

        return value.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(part => part.Split(':', StringSplitOptions.TrimEntries))
            .Where(parts => parts.Length == 2 && int.TryParse(parts[1], out _))
            .ToDictionary(parts => parts[0], parts => int.Parse(parts[1]), StringComparer.OrdinalIgnoreCase);
    }
}
