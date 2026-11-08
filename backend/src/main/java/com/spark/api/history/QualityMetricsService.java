package com.spark.api.history;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QualityMetricsService {
  public record Metrics(long total, long liked, long rated, long sent, long replied) {
    public double likeRate() {
      return rated == 0 ? 0 : (double) liked / rated;
    }

    public double sentRate() {
      return total == 0 ? 0 : (double) sent / total;
    }

    public double repliedRate() {
      return sent == 0 ? 0 : (double) replied / sent;
    }
  }

  private final JdbcTemplate jdbc;

  public QualityMetricsService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Metrics snapshot() {
    return jdbc.queryForObject(
        "select count(*),count(*) filter(where feedback='LIKE'),count(*) filter(where feedback is not null),count(*) filter(where sent_status='SENT'),count(*) filter(where sent_status='SENT' and outcome='REPLIED') from suggestions",
        (rs, n) ->
            new Metrics(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)));
  }

  @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
  public void log() {
    var m = snapshot();
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info(
            "quality_metrics total={} likeRate={} sentRate={} repliedRate={}",
            m.total(),
            m.likeRate(),
            m.sentRate(),
            m.repliedRate());
  }
}
