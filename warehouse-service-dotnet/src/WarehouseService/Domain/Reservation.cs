namespace WarehouseService.Domain;

public sealed record Reservation(
    Guid ReservationId,
    Guid OrderId,
    Guid CorrelationId,
    IReadOnlyList<ReservationItem> Items,
    ReservationStatus Status,
    DateTimeOffset CreatedAt,
    DateTimeOffset? UpdatedAt)
{
    public static Reservation Create(Guid orderId, Guid correlationId, IReadOnlyList<ReservationItem> items)
    {
        if (orderId == Guid.Empty)
        {
            throw new ArgumentException("Order id must not be empty.", nameof(orderId));
        }

        if (correlationId == Guid.Empty)
        {
            throw new ArgumentException("Correlation id must not be empty.", nameof(correlationId));
        }

        if (items.Count == 0)
        {
            throw new ArgumentException("At least one item is required.", nameof(items));
        }

        return new Reservation(
            Guid.NewGuid(),
            orderId,
            correlationId,
            items,
            ReservationStatus.Reserved,
            DateTimeOffset.UtcNow,
            null);
    }

    public Reservation Cancel()
    {
        EnsureReserved();
        return this with { Status = ReservationStatus.Cancelled, UpdatedAt = DateTimeOffset.UtcNow };
    }

    public Reservation Commit()
    {
        EnsureReserved();
        return this with { Status = ReservationStatus.Committed, UpdatedAt = DateTimeOffset.UtcNow };
    }

    private void EnsureReserved()
    {
        if (Status != ReservationStatus.Reserved)
        {
            throw new InvalidOperationException($"Reservation is already {Status}.");
        }
    }
}
