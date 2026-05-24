using System.Text.Json;
using System.Text.Json.Serialization;
using InvoiceService.Application;

namespace InvoiceService.Api.DependencyInjection;

public static class InvoiceApplicationServiceCollectionExtensions
{
    public static IServiceCollection AddInvoiceApplicationServices(this IServiceCollection services)
    {
        services.ConfigureHttpJsonOptions(options =>
        {
            options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
            options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        });

        services.AddScoped<CreateInvoiceUseCase>();
        services.AddScoped<GetInvoiceUseCase>();
        services.AddScoped<SearchInvoicesUseCase>();

        services.AddProblemDetails();
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen();

        return services;
    }
}
