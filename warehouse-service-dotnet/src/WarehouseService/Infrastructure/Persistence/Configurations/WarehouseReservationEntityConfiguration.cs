using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using WarehouseService.Domain;
using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Configurations;

public sealed class WarehouseReservationEntityConfiguration : IEntityTypeConfiguration<WarehouseReservationEntity>
{
    public void Configure(EntityTypeBuilder<WarehouseReservationEntity> builder)
    {
        builder.ToTable("warehouse_reservations");

        builder.HasKey(reservation => reservation.ReservationId);

        builder.Property(reservation => reservation.ReservationId)
            .HasColumnName("reservation_id");

        builder.Property(reservation => reservation.OrderId)
            .HasColumnName("order_id")
            .IsRequired();

        builder.Property(reservation => reservation.CorrelationId)
            .HasColumnName("correlation_id")
            .IsRequired();

        builder.Property(reservation => reservation.Status)
            .HasColumnName("status")
            .HasConversion(status => status.ToString(), value => Enum.Parse<ReservationStatus>(value))
            .HasMaxLength(40)
            .IsRequired();

        builder.Property(reservation => reservation.CreatedAtUtc)
            .HasColumnName("created_at_utc")
            .IsRequired();

        builder.Property(reservation => reservation.UpdatedAtUtc)
            .HasColumnName("updated_at_utc");

        builder.HasMany(reservation => reservation.Items)
            .WithOne(item => item.Reservation)
            .HasForeignKey(item => item.ReservationId);

        builder.HasIndex(reservation => reservation.OrderId)
            .IsUnique()
            .HasDatabaseName("ix_warehouse_reservations_order");

        builder.HasIndex(reservation => reservation.CorrelationId)
            .HasDatabaseName("ix_warehouse_reservations_correlation");
    }
}
