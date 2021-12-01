/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.monitoring;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.Enumeration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

//@Execution(SAME_THREAD) for JUnit5
public class ServerMonitoringMetricsTest {

  @Before
  public void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  public void creatingClassShouldAddMetricsToRegistry() {
    assertThat(sizeOfDefaultRegistry()).isNotPositive();

    new ServerMonitoringMetrics();

    assertThat(sizeOfDefaultRegistry()).isPositive();
  }

  @Test
  public void setters_setGreenStatusForMetricsInTheMetricsRegistry() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setGithubStatusToGreen();
    metrics.setGitlabStatusToGreen();
    metrics.setAzureStatusToGreen();
    metrics.setBitbucketStatusToGreen();

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("github_config_ok")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("gitlab_config_ok")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("bitbucket_config_ok")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("azure_config_ok")).isZero();
  }

  @Test
  public void setters_setRedStatusForMetricsInTheMetricsRegistry() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setGithubStatusToRed();
    metrics.setGitlabStatusToRed();
    metrics.setAzureStatusToRed();
    metrics.setBitbucketStatusToRed();

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("github_config_ok")).isEqualTo(1);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("gitlab_config_ok")).isEqualTo(1);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("bitbucket_config_ok")).isEqualTo(1);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("azure_config_ok")).isEqualTo(1);
  }

  private int sizeOfDefaultRegistry() {
    Enumeration<Collector.MetricFamilySamples> metrics = CollectorRegistry.defaultRegistry.metricFamilySamples();
    return Collections.list(metrics).size();
  }
}