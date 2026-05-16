FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/WarehouseService/WarehouseService.csproj src/WarehouseService/
RUN dotnet restore src/WarehouseService/WarehouseService.csproj

COPY src/WarehouseService/ src/WarehouseService/
RUN dotnet publish src/WarehouseService/WarehouseService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "WarehouseService.dll"]
