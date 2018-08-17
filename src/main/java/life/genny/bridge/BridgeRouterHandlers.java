package life.genny.bridge;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import life.genny.channel.Producer;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.security.SecureResources;
import life.genny.utils.VertxUtils;


public class BridgeRouterHandlers {

	private static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static CorsHandler cors() {
		return CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type")
				.allowedHeader("X-Requested-With");
	}

	public static void apiGetInitHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(bodyy -> {
			final String fullurl = routingContext.request().getParam("url");
			URL aURL = null;
			try {
				aURL = new URL(fullurl);
				final String url = aURL.getHost();
				String key = url + ".json";
				final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(key);
				if (keycloakJsonText != null) {
					final JsonObject retInit = new JsonObject(keycloakJsonText);
					retInit.put("vertx_url", vertxUrl);
					retInit.put("api_url", "https://api-fourdegrees-dev.outcome-hub.com");
					final String kcUrl = retInit.getString("auth-server-url");
					retInit.put("url", kcUrl);
					final String kcClientId = retInit.getString("resource");
					retInit.put("clientId", kcClientId);
					log.info("WEB API GET    >> SETUP REQ:" + url + " sending : " + kcUrl + " " + kcClientId);
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
				} else {
					System.out.println(key + " NOT FOUND IN KEYCLOAK-JSON-MAP");

					// Treat Inbound api call as a WEB SITE!!

					final JsonObject retInit = new JsonObject();
					retInit.put("realm", "www");
					retInit.put("vertx_url", vertxUrl);
					log.info("WEB API GETWWW >> SETUP REQ:" + url + " sending : WWW");
					routingContext.response().putHeader("Content-Type", "application/json");
					routingContext.response().end(retInit.toString());
				}
			} catch (final MalformedURLException e) {
				routingContext.response().end();
			}
			;
		});
	}

	public static void apiInitHandler(final RoutingContext routingContext) {

			routingContext.request().bodyHandler(body -> {
			final String bodyString = body.toString();
			final JsonObject j = new JsonObject(bodyString);
			log.info("WEB API POST   >> SESSION_INIT:");
			//		+ j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1]);
			String tokenSt = j.getJsonObject("headers").getString("Authorization").split("Bearer ")[1];
			JSONObject tokenJSON = KeycloakUtils.getDecodedToken(tokenSt);
			String sessionState = tokenJSON.getString("session_state");
			String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
			String userCode = "PER_" + uname.toUpperCase();

			Set<String> sessionStates = VertxUtils.getSetString("","SessionStates", userCode);
			sessionStates.add(sessionState);
			VertxUtils.putSetString("","SessionStates", userCode, sessionStates);
			final MessageProducer<JsonObject> toSessionChannel =
				          Vertx.currentContext().owner().eventBus().publisher(sessionState);
			VertxUtils.putMessageProducer(sessionState,toSessionChannel);
			routingContext.response().end();

		});
	}



	public static void apiServiceHandler(final RoutingContext routingContext) {
		String token = routingContext.request().getParam("token");
		routingContext.request().bodyHandler(body -> {
			String localToken = null;
			final JsonObject j = body.toJsonObject();
			if (token == null) {
				MultiMap headerMap = routingContext.request().headers();
				localToken = headerMap.get("Authorization");
				if (localToken == null) {
					log.error("NULL TOKEN!");
				} else {
					localToken = localToken.substring(7); // To remove initial [Bearer ]
				}
			} else {
				localToken = token;
			}
			// j.put("token", token);
			System.out.println("Incoming Service:"+j);
			if (j.getString("msg_type").equals("EVT_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS EVENT:" + j);
				j.put("token", localToken);
				final DeliveryOptions options = new DeliveryOptions();
				options.addHeader("Authorization", "Bearer " + localToken);
				Producer.getToEvents().deliveryOptions(options);
				Producer.getToEvents().send(j);
			} else

			if (j.getString("msg_type").equals("CMD_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS CMD  :" + j);
				j.put("token", localToken);
				Producer.getToCmds().send(j);
			} else if (j.getString("msg_type").equals("MSG_MESSAGE")) {
				log.info("CMD API POST   >> EVENT-BUS MSG DATA :" + j);
				j.put("token", localToken);
				Producer.getToMessages().send(j);
			} else if (j.getString("msg_type").equals("DATA_MSG")) {
				log.info("CMD API POST   >> EVENT-BUS DATA :" + j);
				j.put("token", localToken);
				Producer.getToData().send(j);
			}

		});
		routingContext.response().end();
	}

	public static void apiHandler(final RoutingContext routingContext) {
		routingContext.request().bodyHandler(body -> {
			if (body.toJsonObject().getString("msg_type").equals("CMD_MSG"))
				log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + body.toJsonObject());
			Producer.getToClientOutbound().send(body.toJsonObject());
			if (body.toJsonObject().getString("msg_type").equals("DATA_MSG"))
				log.info("EVENT-BUS DATA >> WEBSOCKET DATA:" + body.toJsonObject());
			Producer.getToData().send(body.toJsonObject());
		});
		routingContext.response().end();
	}

}
