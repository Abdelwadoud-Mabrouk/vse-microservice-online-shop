using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Configurations;

public sealed class WarehouseReservationItemEntityConfiguration : IEntityTypeConfiguration<WarehouseReservationItemEntity>
{
    public void Configure(EntityTypeBuilder<WarehouseReservationItemEntity> builder)
    {
        builder.ToTable("warehouse_reservation_items");

        builder.HasKey(item => new { item.ReservationId, item.ProductId });

        builder.Property(item => item.ReservationId)
            .HasColumnName("reservation_id");

        builder.Property(item => item.ProductId)
            .HasColumnName("product_id");

        builder.Property(item => item.Sku)
            .HasColumnName("sku")
            .HasMaxLength(80)
            .IsRequired();

        builder.Property(item => item.Quantity)
            .HasColumnName("quantity")
            .IsRequired();

        builder.ToTable(table => table.HasCheckConstraint("ck_warehouse_reservation_items_quantity", "quantity > 0"));
    }
}
