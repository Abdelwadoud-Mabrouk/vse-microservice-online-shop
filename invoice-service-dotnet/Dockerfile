FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY invoice-service-dotnet/src/InvoiceService/InvoiceService.csproj invoice-service-dotnet/src/InvoiceService/
COPY dotnet-shared/ dotnet-shared/
RUN dotnet restore invoice-service-dotnet/src/InvoiceService/InvoiceService.csproj

COPY invoice-service-dotnet/src/InvoiceService/ invoice-service-dotnet/src/InvoiceService/
RUN dotnet publish invoice-service-dotnet/src/InvoiceService/InvoiceService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "InvoiceService.dll"]
