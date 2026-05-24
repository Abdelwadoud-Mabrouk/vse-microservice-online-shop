using System.Text.Json;
using System.Text.Json.Serialization;
using WarehouseService.Application;

namespace WarehouseService.Api.DependencyInjection;

public static class WarehouseApplicationServiceCollectionExtensions
{
    public static IServiceCollection AddWarehouseApplicationServices(this IServiceCollection services)
    {
        services.ConfigureHttpJsonOptions(options =>
        {
            options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
            options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        });

        services.AddScoped<GetInventoryUseCase>();
        services.AddScoped<ReserveStockUseCase>();
        services.AddScoped<CancelReservationUseCase>();
        services.AddScoped<CommitReservationUseCase>();
        services.AddScoped<AddInventoryItemUseCase>();
        services.AddScoped<UpdateInventoryItemUseCase>();
        services.AddScoped<SetStockQuantityUseCase>();
        services.AddScoped<RemoveInventoryItemUseCase>();

        services.AddProblemDetails();
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen();

        return services;
    }
}
