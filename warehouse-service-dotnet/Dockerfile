FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY warehouse-service-dotnet/src/WarehouseService/WarehouseService.csproj warehouse-service-dotnet/src/WarehouseService/
COPY dotnet-shared/ dotnet-shared/
RUN dotnet restore warehouse-service-dotnet/src/WarehouseService/WarehouseService.csproj

COPY warehouse-service-dotnet/src/WarehouseService/ warehouse-service-dotnet/src/WarehouseService/
RUN dotnet publish warehouse-service-dotnet/src/WarehouseService/WarehouseService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "WarehouseService.dll"]
