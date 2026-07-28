using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;

namespace WarehouseService.Infrastructure.Persistence.Migrations;

[DbContext(typeof(WarehouseDbContext))]
public sealed class WarehouseDbContextModelSnapshot : ModelSnapshot
{
    protected override void BuildModel(ModelBuilder modelBuilder)
    {
        modelBuilder
            .HasAnnotation("ProductVersion", "10.0.4");

        modelBuilder.Entity("WarehouseService.Infrastructure.Persistence.Entities.WarehouseProductEntity", builder =>
        {
            builder.Property<Guid>("ProductId")
                .HasColumnName("product_id");

            builder.Property<int>("AvailableQuantity")
                .HasColumnName("available_quantity");

            builder.Property<string>("Category")
                .IsRequired()
                .HasMaxLength(80)
                .HasColumnName("category");

            builder.Property<string>("Name")
                .IsRequired()
                .HasMaxLength(160)
                .HasColumnName("name");

            builder.Property<string>("Sku")
                .IsRequired()
                .HasMaxLength(80)
                .HasColumnName("sku");

            builder.HasKey("ProductId")
                .HasName("pk_warehouse_products");

            builder.HasIndex("Sku")
                .IsUnique()
                .HasDatabaseName("ix_warehouse_products_sku");

            builder.ToTable("warehouse_products", table =>
            {
                table.HasCheckConstraint("ck_warehouse_products_available_quantity", "available_quantity >= 0");
            });
        });

        modelBuilder.Entity("WarehouseService.Infrastructure.Persistence.Entities.WarehouseReservationEntity", builder =>
        {
            builder.Property<Guid>("ReservationId")
                .HasColumnName("reservation_id");

            builder.Property<Guid>("CorrelationId")
                .HasColumnName("correlation_id");

            builder.Property<DateTimeOffset>("CreatedAtUtc")
                .HasColumnName("created_at_utc");

            builder.Property<Guid>("OrderId")
                .HasColumnName("order_id");

            builder.Property<string>("Status")
                .IsRequired()
                .HasMaxLength(40)
                .HasColumnName("status");

            builder.Property<DateTimeOffset?>("UpdatedAtUtc")
                .HasColumnName("updated_at_utc");

            builder.HasKey("ReservationId")
                .HasName("pk_warehouse_reservations");

            builder.HasIndex("CorrelationId")
                .HasDatabaseName("ix_warehouse_reservations_correlation");

            builder.HasIndex("OrderId")
                .IsUnique()
                .HasDatabaseName("ix_warehouse_reservations_order");

            builder.ToTable("warehouse_reservations");
        });

        modelBuilder.Entity("WarehouseService.Infrastructure.Persistence.Entities.WarehouseReservationItemEntity", builder =>
        {
            builder.Property<Guid>("ReservationId")
                .HasColumnName("reservation_id");

            builder.Property<Guid>("ProductId")
                .HasColumnName("product_id");

            builder.Property<int>("Quantity")
                .HasColumnName("quantity");

            builder.Property<string>("Sku")
                .IsRequired()
                .HasMaxLength(80)
                .HasColumnName("sku");

            builder.HasKey("ReservationId", "ProductId")
                .HasName("pk_warehouse_reservation_items");

            builder.ToTable("warehouse_reservation_items", table =>
            {
                table.HasCheckConstraint("ck_warehouse_reservation_items_quantity", "quantity > 0");
            });
        });

        modelBuilder.Entity("WarehouseService.Infrastructure.Persistence.Entities.WarehouseReservationItemEntity", builder =>
        {
            builder.HasOne("WarehouseService.Infrastructure.Persistence.Entities.WarehouseReservationEntity", "Reservation")
                .WithMany("Items")
                .HasForeignKey("ReservationId")
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired()
                .HasConstraintName("fk_warehouse_reservation_items_warehouse_reservations_reservation_id");

            builder.Navigation("Reservation");
        });

        modelBuilder.Entity("WarehouseService.Infrastructure.Persistence.Entities.WarehouseReservationEntity", builder =>
        {
            builder.Navigation("Items");
        });
    }
}
