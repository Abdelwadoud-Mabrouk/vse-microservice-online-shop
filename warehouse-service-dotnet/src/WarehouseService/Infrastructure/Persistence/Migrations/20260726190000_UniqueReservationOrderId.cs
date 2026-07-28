using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Infrastructure;

#nullable disable

namespace WarehouseService.Infrastructure.Persistence.Migrations;

[DbContext(typeof(WarehouseDbContext))]
[Migration("20260726190000_UniqueReservationOrderId")]
public partial class UniqueReservationOrderId : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropIndex("ix_warehouse_reservations_order", "warehouse_reservations");
        migrationBuilder.CreateIndex(
            name: "ix_warehouse_reservations_order",
            table: "warehouse_reservations",
            column: "order_id",
            unique: true);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropIndex("ix_warehouse_reservations_order", "warehouse_reservations");
        migrationBuilder.CreateIndex(
            name: "ix_warehouse_reservations_order",
            table: "warehouse_reservations",
            column: "order_id");
    }
}
