using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace WarehouseService.Infrastructure.Persistence.Migrations;

public partial class InitialWarehouseSchema : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.CreateTable(
            name: "warehouse_products",
            columns: table => new
            {
                product_id = table.Column<Guid>(type: "uuid", nullable: false),
                sku = table.Column<string>(type: "character varying(80)", maxLength: 80, nullable: false),
                name = table.Column<string>(type: "character varying(160)", maxLength: 160, nullable: false),
                category = table.Column<string>(type: "character varying(80)", maxLength: 80, nullable: false),
                available_quantity = table.Column<int>(type: "integer", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("pk_warehouse_products", x => x.product_id);
                table.CheckConstraint("ck_warehouse_products_available_quantity", "available_quantity >= 0");
            });

        migrationBuilder.CreateTable(
            name: "warehouse_reservations",
            columns: table => new
            {
                reservation_id = table.Column<Guid>(type: "uuid", nullable: false),
                order_id = table.Column<Guid>(type: "uuid", nullable: false),
                correlation_id = table.Column<Guid>(type: "uuid", nullable: false),
                status = table.Column<string>(type: "character varying(40)", maxLength: 40, nullable: false),
                created_at_utc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                updated_at_utc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true)
            },
            constraints: table =>
            {
                table.PrimaryKey("pk_warehouse_reservations", x => x.reservation_id);
            });

        migrationBuilder.CreateTable(
            name: "warehouse_reservation_items",
            columns: table => new
            {
                reservation_id = table.Column<Guid>(type: "uuid", nullable: false),
                product_id = table.Column<Guid>(type: "uuid", nullable: false),
                sku = table.Column<string>(type: "character varying(80)", maxLength: 80, nullable: false),
                quantity = table.Column<int>(type: "integer", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("pk_warehouse_reservation_items", x => new { x.reservation_id, x.product_id });
                table.CheckConstraint("ck_warehouse_reservation_items_quantity", "quantity > 0");
                table.ForeignKey(
                    name: "fk_warehouse_reservation_items_warehouse_reservations_reservation_id",
                    column: x => x.reservation_id,
                    principalTable: "warehouse_reservations",
                    principalColumn: "reservation_id",
                    onDelete: ReferentialAction.Cascade);
            });

        migrationBuilder.CreateIndex(
            name: "ix_warehouse_products_sku",
            table: "warehouse_products",
            column: "sku",
            unique: true);

        migrationBuilder.CreateIndex(
            name: "ix_warehouse_reservations_correlation",
            table: "warehouse_reservations",
            column: "correlation_id");

        migrationBuilder.CreateIndex(
            name: "ix_warehouse_reservations_order",
            table: "warehouse_reservations",
            column: "order_id");

        migrationBuilder.InsertData(
            table: "warehouse_products",
            columns: new[] { "product_id", "sku", "name", "category", "available_quantity" },
            values: new object[,]
            {
                { Guid.Parse("11111111-1111-4111-8111-111111111111"), "SKU-CHAIR-OAK", "Esszimmerstuhl Eiche", "Stuehle", 24 },
                { Guid.Parse("22222222-2222-4222-8222-222222222222"), "SKU-DESK-OSLO", "Schreibtisch Oslo", "Tische", 12 },
                { Guid.Parse("33333333-3333-4333-8333-333333333333"), "SKU-LAMP-LUNA", "Stehlampe Luna", "Lampen", 30 },
                { Guid.Parse("44444444-4444-4444-8444-444444444444"), "SKU-SOFA-NORDIC", "Dreisitzer Sofa Nordic", "Sofas", 8 },
                { Guid.Parse("55555555-5555-4555-8555-555555555555"), "SKU-BED-LINA", "Doppelbett Lina 180x200", "Betten", 7 },
                { Guid.Parse("66666666-6666-4666-8666-666666666666"), "SKU-WARDROBE-MALMO", "Kleiderschrank Malmo", "Schraenke", 6 },
                { Guid.Parse("77777777-7777-4777-8777-777777777777"), "SKU-SHELF-BERGEN", "Buecherregal Bergen", "Regale", 18 },
                { Guid.Parse("88888888-8888-4888-8888-888888888888"), "SKU-TABLE-ROUND", "Runder Esstisch Mira", "Tische", 9 },
                { Guid.Parse("99999999-9999-4999-8999-999999999999"), "SKU-COFFEE-TABLE", "Couchtisch Fino", "Tische", 15 },
                { Guid.Parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), "SKU-TV-BOARD", "TV-Board Bergen", "Sideboards", 11 },
                { Guid.Parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), "SKU-NIGHTSTAND", "Nachttisch Liva", "Schlafzimmer", 20 },
                { Guid.Parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"), "SKU-SIDEBOARD-NOVA", "Sideboard Nova", "Sideboards", 10 },
                { Guid.Parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"), "SKU-OFFICE-CHAIR", "Buerostuhl Ergo", "Stuehle", 16 },
                { Guid.Parse("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"), "SKU-DINING-BENCH", "Essbank Kopenhagen", "Sitzbaenke", 10 },
                { Guid.Parse("ffffffff-ffff-4fff-8fff-ffffffffffff"), "SKU-SHOE-CABINET", "Schuhschrank Fynn", "Flur", 14 },
                { Guid.Parse("12121212-1212-4212-8212-121212121212"), "SKU-DRESSER-ELLA", "Kommode Ella", "Schraenke", 13 },
                { Guid.Parse("34343434-3434-4434-8434-343434343434"), "SKU-ARMCHAIR-MIKA", "Sessel Mika", "Sessel", 9 },
                { Guid.Parse("56565656-5656-4565-8565-565656565656"), "SKU-BAR-STOOL", "Barhocker Loft", "Stuehle", 22 },
                { Guid.Parse("78787878-7878-4787-8787-787878787878"), "SKU-GARDEN-CHAIR", "Gartenstuhl Palma", "Garten", 28 },
                { Guid.Parse("90909090-9090-4909-8909-909090909090"), "SKU-GARDEN-TABLE", "Gartentisch Palma", "Garten", 8 }
            });
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropTable(name: "warehouse_reservation_items");
        migrationBuilder.DropTable(name: "warehouse_products");
        migrationBuilder.DropTable(name: "warehouse_reservations");
    }
}
