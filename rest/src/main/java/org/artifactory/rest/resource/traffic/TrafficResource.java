/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.rest.resource.traffic;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.rest.TrafficRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.traffic.TrafficService;
import org.artifactory.traffic.entry.TrafficEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Noam Tenne
 */
@Path("/" + TrafficRestConstants.TRAFFIC_PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TrafficResource {

    @Context
    private HttpServletResponse httpResponse;

    @Autowired
    private TrafficService trafficService;

    @GET
    @Path("/" + TrafficRestConstants.TRAFFIC_STREAM_ROOT)
    @Produces("text/plain")
    public String getTrafficLogFilesStream(
            @QueryParam(TrafficRestConstants.TRAFFIC_PARAM_START_DATE) long startLong,
            @QueryParam(TrafficRestConstants.TRAFFIC_PARAM_END_DATE) long endLong) throws IOException {
        Date startDate = new Date(startLong);
        Date endDate = new Date(endLong);
        validateDateRange(startDate, endDate);
        Calendar from = Calendar.getInstance();
        from.setTimeInMillis(startLong);
        Calendar to = Calendar.getInstance();
        to.setTimeInMillis(endLong);
        List<TrafficEntry> trafficEntryList = trafficService.getEntryList(from, to);
        writeEntriesToStream(trafficEntryList);

        return "";
    }

    private void validateDateRange(Date startDate, Date endDate) {
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("The start date cannot be later than the end date");
        }
    }

    private void writeEntriesToStream(List<TrafficEntry> trafficEntryList) throws IOException {
        if (!trafficEntryList.isEmpty()) {
            OutputStreamWriter writer = new OutputStreamWriter(httpResponse.getOutputStream());
            try {
                for (TrafficEntry trafficEntry : trafficEntryList) {
                    String lineToWrite = (trafficEntry.toString() + "\n");
                    IOUtils.write(lineToWrite, writer);
                }
            } finally {
                writer.close();
            }
        }
    }
}