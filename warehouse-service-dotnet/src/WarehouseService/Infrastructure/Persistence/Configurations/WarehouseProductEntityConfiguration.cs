using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Configurations;

public sealed class WarehouseProductEntityConfiguration : IEntityTypeConfiguration<WarehouseProductEntity>
{
    public void Configure(EntityTypeBuilder<WarehouseProductEntity> builder)
    {
        builder.ToTable("warehouse_products");

        builder.HasKey(product => product.ProductId);

        builder.Property(product => product.ProductId)
            .HasColumnName("product_id");

        builder.Property(product => product.Sku)
            .HasColumnName("sku")
            .HasMaxLength(80)
            .IsRequired();

        builder.Property(product => product.Name)
            .HasColumnName("name")
            .HasMaxLength(160)
            .IsRequired();

        builder.Property(product => product.Category)
            .HasColumnName("category")
            .HasMaxLength(80)
            .IsRequired();

        builder.Property(product => product.AvailableQuantity)
            .HasColumnName("available_quantity")
            .IsRequired();

        builder.HasIndex(product => product.Sku)
            .IsUnique();

        builder.ToTable(table => table.HasCheckConstraint("ck_warehouse_products_available_quantity", "available_quantity >= 0"));

        builder.HasData(WarehouseProductSeed.Products);
    }
}
