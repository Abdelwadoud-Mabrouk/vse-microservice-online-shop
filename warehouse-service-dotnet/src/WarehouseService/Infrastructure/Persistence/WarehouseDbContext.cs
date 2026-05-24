using Microsoft.EntityFrameworkCore;
using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence;

public sealed class WarehouseDbContext(DbContextOptions<WarehouseDbContext> options) : DbContext(options)
{
    public DbSet<WarehouseProductEntity> Products => Set<WarehouseProductEntity>();

    public DbSet<WarehouseReservationEntity> Reservations => Set<WarehouseReservationEntity>();

    public DbSet<WarehouseReservationItemEntity> ReservationItems => Set<WarehouseReservationItemEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(WarehouseDbContext).Assembly);
    }
}
