package com.sas.coeci.esp.rdm;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.pmw.tinylog.Logger;

import com.sas.esp.api.dfESPException;
import com.sas.esp.api.server.datavar;
import com.sas.esp.api.server.ReferenceIMPL.dfESPevent;
import com.sas.esp.api.server.ReferenceIMPL.dfESPschema;
import com.sas.tap.client.SASDSRequest;
import com.sas.tap.client.SASDSRequestFactory;
import com.sas.tap.client.SASDSResponse;

public class RDMEngine {

	private String host;
	private int port;
	private Properties props;

	public RDMEngine(String _host, int _port, Properties _props) {
		host = _host;
		port = _port;
		props = _props;
	}

	public SASDSResponse invokeRdm(String eventName, dfESPevent event, dfESPschema schema, String correlationId, String timezone) throws SecurityException,
			IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {

		SASDSRequestFactory factory = SASDSRequestFactory.getInstance(getRdmUrl(), props);
		SASDSRequest request = factory.create(eventName, correlationId, timezone);

		int rdmParameterCount = schema.getNumFields();

		for (int i = 0; i < rdmParameterCount; i++) {
			String parameterName = schema.getNames().get(i);
			String parameterType = "";
			try {
				datavar parameterData = event.copyByExtID(schema, i);

				switch (parameterData.getType()) {
				case TIMESTAMP:
				case DATETIME:
					// treat as Date
					parameterType = "Date";
					GregorianCalendar c = new GregorianCalendar();
					c.setTime((Date) parameterData.getValue());
					XMLGregorianCalendar xmlGregCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
					request.setXMLGregorianCalendar(parameterName, xmlGregCal);
					break;
				case INT32:
					// treat as Integer but cast to Long
					parameterType = "Integer";
					request.setLong(parameterName, ((Integer) parameterData.getValue()).longValue());
					Logger.debug("Adding RTDM Input-Variable {}:{} = {}", parameterName, parameterType, ((Integer) parameterData.getValue()).longValue());
					break;
				case INT64:
					// treat as Long.
					parameterType = "Long";
					request.setLong(parameterName, (Long) parameterData.getValue());
					Logger.debug("Adding RTDM Input-Variable {}:{} = {}", parameterName, parameterType, (Long) parameterData.getValue());
					break;
				case DOUBLE:
				case MONEY:
					// treat as double
					parameterType = "Double";
					request.setDouble(parameterName, (Double) parameterData.getValue());
					Logger.debug("Adding RTDM Input-Variable {}:{} = {}", parameterName, parameterType, (Double) parameterData.getValue());
					break;
				case UTF8STR:
					// treat as string
					parameterType = "String";
					request.setString(parameterName, (String) parameterData.getValue());
					Logger.debug("Adding RTDM Input-Variable {}:{} = {}", parameterName, parameterType, (String) parameterData.getValue());
					break;
				default:
					Logger.warn("Event Parameter {} Datatype {} is not supported. The value is not transmitted to RTDM.", parameterName,
							parameterData.getType());
					break;
				} // Switch

			} catch (dfESPException | DatatypeConfigurationException | ClassCastException e) {
				Logger.error("Error happening while creating Parameter {} ", parameterName);
				Logger.error(e);
			} // Try Catch
		} // For Loop

		return null;
		// SASDSResponse response = request.execute();
		// return response;

	}

	public SASDSResponse invokeRdm(String eventName, List<RDMParameter<?>> parameterList, String corrleationId, String timezone) throws ClassNotFoundException,
			SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException,
			DatatypeConfigurationException {

		SASDSRequestFactory factory = SASDSRequestFactory.getInstance(getRdmUrl(), props);
		SASDSRequest request = factory.create(eventName, corrleationId, timezone);

		for (RDMParameter<?> parameter : parameterList) {

			switch (parameter.getType()) {
			case String:
				request.setString(parameter.getName(), (String) parameter.getValue());
				break;
			case Long:
				request.setLong(parameter.getName(), (Long) parameter.getValue());
				break;
			case Double:
				request.setDouble(parameter.getName(), (Double) parameter.getValue());
				break;
			case Boolean:
				request.setBoolean(parameter.getName(), (Boolean) parameter.getValue());
				break;
			case DateTime:
				// Cast to XMLGregCalendar
				GregorianCalendar c = new GregorianCalendar();
				c.setTime((Date) parameter.getValue());
				XMLGregorianCalendar xmlGregCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
				request.setXMLGregorianCalendar(parameter.getName(), xmlGregCal);
				break;
			default:

				break;
			}
		}
		SASDSResponse response = request.execute();

		return response;
	}

	private String getRdmUrl() {
		return "http://" + host + ":" + port + "/RTDM/Custom";
	}

	public static String getCorrelationId() {
		return Long.toHexString(System.currentTimeMillis());
	}

	public static String getTimezone() {
		return "GMT";
	}
}
