package org.opentripplanner.ext.velotmbikerental;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.vehicle_rental.datasources.GenericJsonVehicleRentalDataSource;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VeloTmBikeRentalDataSource extends GenericJsonVehicleRentalDataSource<VeloTmBikeRentalDataSourceParameters> {
    private static final Logger log = LoggerFactory.getLogger(VeloTmBikeRentalDataSource.class);

    public static final String DEFAULT_NETWORK_NAME = "velotm";

    private final String networkName;
    /**
     * Construct superclass
     *
     * @param config
     */
    public VeloTmBikeRentalDataSource(VeloTmBikeRentalDataSourceParameters config) {
        super(config, "Data");
        networkName = config.getNetwork(DEFAULT_NETWORK_NAME);
    }

    @Override
    public boolean update() {
        var url = config.getUrl();
        var headers = config.getHttpHeaders();

        try {
            InputStream data;

            URL url2 = new URL(url);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                HttpPost httpPost = new HttpPost(URI.create(url));
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
                var httpclient = HttpClientBuilder.create()
                        .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(5000).build())
                        .setConnectionTimeToLive(5000, TimeUnit.MILLISECONDS)
                        .build();

                HttpResponse response = httpclient.execute(httpPost);
                if(response.getStatusLine().getStatusCode() != 200) {
                    return false;
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return false;
                }

                data = entity.getContent();
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }
            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseJSON(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing vehicle rental feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing vehicle rental feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading vehicle rental feed from " + url, e);
            return false;
        }
        return true;
    }

    @Override
    public VehicleRentalPlace makeStation(JsonNode rentalStationNode) {
        if (!Objects.equals(rentalStationNode.path("Status").asText(), "Functionala")) {
            return null;
        }

        if (!Objects.equals(rentalStationNode.path("IsValid").asText(), "true")) {
            return null;
        }

        VehicleRentalStation station = new VehicleRentalStation();

        station.id = new FeedScopedId(networkName, rentalStationNode.path("Id").toString());
        station.longitude = rentalStationNode.path("Longitude").asDouble();
        station.latitude = rentalStationNode.path("Latitude").asDouble();
        station.name =  new NonLocalizedString(rentalStationNode.path("StationName").asText());
        station.vehiclesAvailable = rentalStationNode.path("OcuppiedSpots").asInt();
        station.spacesAvailable = rentalStationNode.path("EmptySpots").asInt();
        station.realTimeData = true;

        if (station.latitude == 0 && station.longitude == 0) {
            return null;
        }

        return station;
    }
}
