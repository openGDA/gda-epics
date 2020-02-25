/*-
 * Copyright © 2020 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package uk.ac.gda.epics.archiverclient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gda.factory.Findable;

/**
 * Provides access to the EPICS Archiver REST API. Can be used to retrieve the whole record for a given PV, filtered by start and finish dates and times if required, or to
 * retrieve individual PV values for a given date and time.
 *
 * @see <a href="http://archappl.diamond.ac.uk">EPICS Archiver Appliance for Diamond Light Source</a>
 * @see <a href="https://confluence.diamond.ac.uk/display/CNTRLS/Archiver+Appliance+Overview">Archiver Appliance Overview</a>
 *
 * @author too27251
 */

public class EpicsArchiverClient implements Findable {

	private String name;
	private final String archiverUrl;

	private final RestTemplate restTemplate = new RestTemplate();

	private static final DateTimeFormatter API_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static final DateTimeFormatter INCOMING_DATE_TIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String PV_PARAMETER_NAME = "pv";
	private static final String FROM_PARAMETER_NAME = "from";
	private static final String TO_PARAMETER_NAME = "to";

	private static final String DATA_RETRIEVAL_ENDPOINT = String.format("/getData.json?%s={%s}", PV_PARAMETER_NAME, PV_PARAMETER_NAME);
	private static final String FROM_PARAMETER_SUFFIX = String.format("&%s={%s}", FROM_PARAMETER_NAME, FROM_PARAMETER_NAME);
	private static final String TO_PARAMETER_SUFFIX = String.format("&%s={%s}", TO_PARAMETER_NAME, TO_PARAMETER_NAME);

	private static final Logger logger = LoggerFactory.getLogger(EpicsArchiverClient.class);

	public EpicsArchiverClient(String archiverUrl) {

		Objects.requireNonNull(archiverUrl);
		if (archiverUrl.isEmpty()) {
			throw new IllegalArgumentException("Epics Archiver URL cannot be empty.");
		}
		this.archiverUrl = cleanArchiverUrl(archiverUrl);
	}

	private String cleanArchiverUrl(String url) {
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Queries the EPICS archiver API for a specific PV name and returns all data.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for.
	 * @return An {@link EpicsArchiverRecord} representing the archived data for the queried PV.
	 * @throws EpicsArchiverClientException
	 */
	public Optional<EpicsArchiverRecord> getRecordForPv(String pvName) throws EpicsArchiverClientException {

		String apiUrl = archiverUrl + DATA_RETRIEVAL_ENDPOINT;

		Map<String, String> parameters = new HashMap<>();
		parameters.put(PV_PARAMETER_NAME, pvName);

		EpicsArchiverRecord[] data = queryApi(apiUrl, parameters);

		return firstRecordOrEmpty(data);
	}

	/**
	 * Queries the EPICS archiver API for a specific PV name and returns all date after the starting time-stamp.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for.
	 * @param from - The starting date and time from which you would like data.
	 * @return An {@link EpicsArchiverRecord} representing the archived data for the queried PV.
	 * @throws EpicsArchiverClientException
	 */
	public Optional<EpicsArchiverRecord> getRecordForPv(String pvName, LocalDateTime from) throws EpicsArchiverClientException {

		String apiUrl = archiverUrl + DATA_RETRIEVAL_ENDPOINT + FROM_PARAMETER_SUFFIX;

		Map<String, String> parameters = new HashMap<>();
		parameters.put(PV_PARAMETER_NAME, pvName);
		parameters.put(FROM_PARAMETER_NAME, convertLocalDateTimeToUtcApiString(from));

		EpicsArchiverRecord[] data = queryApi(apiUrl, parameters);

		return firstRecordOrEmpty(data);
	}

	/**
	 * Queries the EPICS archiver API for a specific PV name and returns all data between the start and end time-stamps.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for.
	 * @param from - The starting date and time from which you would like data.
	 * @param to - The end date and time to which you would like data.
	 * @return An {@link EpicsArchiverRecord} representing the archived data for the queried PV.
	 * @throws EpicsArchiverClientException
	 */
	public Optional<EpicsArchiverRecord> getRecordForPv(String pvName, LocalDateTime from, LocalDateTime to) throws EpicsArchiverClientException {

		String apiUrl = archiverUrl + DATA_RETRIEVAL_ENDPOINT + FROM_PARAMETER_SUFFIX + TO_PARAMETER_SUFFIX;

		Map<String, String> parameters = new HashMap<>();
		parameters.put(PV_PARAMETER_NAME, pvName);
		parameters.put(FROM_PARAMETER_NAME, convertLocalDateTimeToUtcApiString(from));
		parameters.put(TO_PARAMETER_NAME, convertLocalDateTimeToUtcApiString(to));

		EpicsArchiverRecord[] data = queryApi(apiUrl, parameters);

		return firstRecordOrEmpty(data);
	}

	private String convertLocalDateTimeToUtcApiString(LocalDateTime dateTime) {

		LocalDateTime utcDateTime = dateTime
				.atZone(ZoneId.systemDefault())
				.withZoneSameInstant(ZoneOffset.UTC)
				.toLocalDateTime();

		return API_DATE_TIME_FORMATTER.format(utcDateTime);
	}

	private EpicsArchiverRecord[] queryApi(String url, Map<String, String> parameters) throws EpicsArchiverClientException {

		ResponseEntity<EpicsArchiverRecord[]> response = null;
		try {
			response = restTemplate.getForEntity(url, EpicsArchiverRecord[].class, parameters);
		} catch (HttpClientErrorException exception) {

			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				logger.warn("Either Archiver API not found, or PV {} not found in archiver. Impossible to tell which!", parameters.get(PV_PARAMETER_NAME));
				return new EpicsArchiverRecord[0];
			}

			throw new EpicsArchiverClientException("Error contacting EPICS archiver API", exception);

		} catch (RestClientException exception) {
			throw new EpicsArchiverClientException("Error contacting EPICS archiver API", exception);
		}

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		}

		throw new EpicsArchiverClientException("Error from EPICS archiver API: " + response.getStatusCode(), null);
	}

	private Optional<EpicsArchiverRecord> firstRecordOrEmpty(EpicsArchiverRecord[] data) {

		if (data.length > 0) {
			return Optional.of(data[0]);
		}

		logger.warn("PV available in archive but no data for selected dates.");
		return Optional.empty();
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for.
	 * @param dateTime - The date and time for which you would like the PV value.
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public OptionalDouble getOptionalValueForPv(String pvName, LocalDateTime dateTime) throws EpicsArchiverClientException {
		Optional<EpicsArchiverRecord> record = getRecordForPv(pvName, dateTime, dateTime);

		if (record.isPresent()) {
			Optional<EpicsArchiverRecordData> dataPoint = record.get()
					.getData()
					.stream()
					.findFirst();

			if (dataPoint.isPresent()) {
				return OptionalDouble.of(dataPoint.get().getVal());
			}
		}

		return OptionalDouble.empty();
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for
	 * @param year - The year for which you would like the PV value.
	 * @param month - The month for which you would like the PV value.
	 * @param day - The day of the month for which you would like the PV value.
	 * @param hour - The hour of the day for which you would like the PV value.
	 * @param minute - The minute of the hour for which you would like the PV value.
	 * @param second - The year for which you would like the PV value.
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public OptionalDouble getOptionalValueForPv(String pvName, int year, int month, int day, int hour, int minute, int second) throws EpicsArchiverClientException {
		return getOptionalValueForPv(pvName, LocalDateTime.of(year, month, day, hour, minute, second));
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for
	 * @param dateTime - The date and time for which you would like the PV value, as a string in the format "yyyy-MM-dd HH:mm:ss".
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public OptionalDouble getOptionalValueForPv(String pvName, String dateTime) throws EpicsArchiverClientException {
		return getOptionalValueForPv(pvName, LocalDateTime.parse(dateTime, INCOMING_DATE_TIME_PARSER));
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time. Throws an exception if not found.
	 * Using getOptionalValueForPv is probably preferable, these are intended for use over RMI ({@link Optional} is not serializable,
	 * or for people who with to use the client from the Jython console and don't want to deal with Optional.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for.
	 * @param dateTime - The date and time for which you would like the PV value.
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public double getValueForPv(String pvName, LocalDateTime dateTime) throws EpicsArchiverClientException {

		return getOptionalValueForPv(pvName, dateTime)
				.orElseThrow(() -> new EpicsArchiverClientException("PV value not present in EPICS archiver."));
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time. Throws an exception if not found.
	 * Using getOptionalValueForPv is probably preferable, these are intended for use over RMI ({@link Optional} is not serializable,
	 * or for people who with to use the client from the Jython console and don't want to deal with Optional.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for
	 * @param year - The year for which you would like the PV value.
	 * @param month - The month for which you would like the PV value.
	 * @param day - The day of the month for which you would like the PV value.
	 * @param hour - The hour of the day for which you would like the PV value.
	 * @param minute - The minute of the hour for which you would like the PV value.
	 * @param second - The year for which you would like the PV value.
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public double getValueForPv(String pvName, int year, int month, int day, int hour, int minute, int second) throws EpicsArchiverClientException {
		return getValueForPv(pvName, LocalDateTime.of(year, month, day, hour, minute, second));
	}

	/**
	 * Queries the EPICS archiver API for a specific PV's value on a specific date and time. Throws an exception if not found.
	 * Using getOptionalValueForPv is probably preferable, these are intended for use over RMI ({@link Optional} is not serializable,
	 * or for people who with to use the client from the Jython console and don't want to deal with Optional.
	 *
	 * @param pvName - The name of the PV you wish to query the archiver for
	 * @param dateTime - The date and time for which you would like the PV value, as a string in the format "yyyy-MM-dd HH:mm:ss".
	 * @return An {@link OptionalDouble} representing the value.
	 * @throws EpicsArchiverClientException
	 */
	public double getValueForPv(String pvName, String dateTime) throws EpicsArchiverClientException {
		return getValueForPv(pvName, LocalDateTime.parse(dateTime, INCOMING_DATE_TIME_PARSER));
	}
}
