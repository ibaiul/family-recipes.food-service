package eus.ibai.family.recipes.food.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@Slf4j
@Component
public class InboundErrorResponseLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    if (statusCode == null || statusCode.isError()) {
                        int status = statusCode != null ? statusCode.value() : 0;
                        RequestPath requestPath = exchange.getRequest().getPath();
                        HttpMethod method = exchange.getRequest().getMethod();
                        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
                        String clientIp = getClientIp(exchange);
                        String userAgent = getUserAgent(exchange);
                        log.info("Inbound error response. Status: {}, Method: {}, Uri: {}, Params: {}, ClientIp: {}, Agent: {}", status, method, requestPath.value(), queryParams, clientIp, userAgent);
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getHeaders().get("X-Forwarded-For"))
                .map(values -> values.get(0))
                .orElseGet(() -> getClientIpFromRequestData(exchange));
    }

    private String getClientIpFromRequestData(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse(null);
    }

    private String getUserAgent(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getHeaders().get("User-Agent"))
                .map(values -> values.get(0))
                .orElse(null);
    }
}
