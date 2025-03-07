/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class QualityProfileRuleChangeResolver {
  private final DbClient dbClient;

  public QualityProfileRuleChangeResolver(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Returns a text description of the changes made to a quality profile.
   * The text is generated by looking at the last change for each rule and determining the final status of the rule.
   * For old taxonomy, we need to access the QProfileChangeDto.data field to determine the ruleUuid.
   * For CCT, we can use the QProfileChangeDto.ruleChange field to determine the ruleUuid.
   *
   * @param profile the quality profile to generate the text for
   * @return a text description of the changes made to the profile
   * @throws IllegalStateException if no changes are found for the profile
   */
  public Map<ActiveRuleChange.Type, Long> mapChangeToNumberOfRules(QualityProfile profile, String componentUuid) {
    // get profile changes
    List<QProfileChangeDto> profileChanges = getProfileChanges(profile, componentUuid);

    Map<String, List<QProfileChangeDto>> updatedRulesGrouped = profileChanges.stream()
      .filter(QualityProfileRuleChangeResolver::hasRuleUuid)
      .collect(Collectors.groupingBy(p -> p.getRuleChange() != null ? p.getRuleChange().getRuleUuid() : p.getDataAsMap().get("ruleUuid")));

    Map<String, ActiveRuleChange.Type> rulesMappedToFinalChange = getRulesMappedToFinalChange(updatedRulesGrouped);
    return getChangeMappedToNumberOfRules(rulesMappedToFinalChange);
  }

  @NotNull
  private static Map<ActiveRuleChange.Type, Long> getChangeMappedToNumberOfRules(Map<String, ActiveRuleChange.Type> rulesMappedToFinalChange) {
    return rulesMappedToFinalChange.values().stream()
      .collect(Collectors.groupingBy(
        actionType -> actionType,
        Collectors.counting()
      ));
  }

  private static boolean hasRuleUuid(QProfileChangeDto change) {
    return (change.getRuleChange() != null && change.getRuleChange().getRuleUuid() != null) ||
      (!change.getDataAsMap().isEmpty() && change.getDataAsMap().containsKey("ruleUuid"));
  }

  /**
   * Returns a map of ruleUuid to the final status of the rule.
   * If the rule final status is the same as the initial status, the value will be empty.
   *
   * @param updatedRulesGrouped a map of ruleUuid to a list of changes for that rule
   * @return a map of ruleUuid to the final status of the rule. If the rule final status is the same as the initial status, the value will be empty.
   */
  private static Map<String, ActiveRuleChange.Type> getRulesMappedToFinalChange(Map<String, List<QProfileChangeDto>> updatedRulesGrouped) {
    return updatedRulesGrouped.entrySet().stream()
      .map(entry -> {
        String key = entry.getKey();
        List<QProfileChangeDto> ruleChanges = entry.getValue();

        // get last change
        QProfileChangeDto lastChange = ruleChanges.stream().max(Comparator.comparing(QProfileChangeDto::getCreatedAt)).orElseThrow();
        Optional<ActiveRuleChange.Type> value;

        if (UPDATED.name().equals(lastChange.getChangeType())) {
          value = Optional.of(UPDATED);
        } else {
          // for ACTIVATED/DEACTIVATED we need to count the number of times the rule was toggled
          long activationToggles = ruleChanges.stream()
            .filter(rule -> List.of(ACTIVATED.name(), DEACTIVATED.name()).contains(rule.getChangeType()))
            .count();
          // If the count is even, skip all rules in this group as the status is unchanged
          // If the count is odd we only care about the last status update
          value = activationToggles % 2 == 0 ? Optional.empty() : Optional.of(ActiveRuleChange.Type.valueOf(lastChange.getChangeType()));
        }

        return new AbstractMap.SimpleEntry<>(key, value);
      })
      .filter(entry -> entry.getValue().isPresent())
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> entry.getValue().get()
      ));
  }

  private List<QProfileChangeDto> getProfileChanges(QualityProfile profile, String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileChangeQuery query = new QProfileChangeQuery(profile.getQpKey());
      query.setFromIncluded(getLastAnalysisDate(componentUuid, dbSession));
      List<QProfileChangeDto> profileChanges = dbClient.qProfileChangeDao().selectByQuery(dbSession, query);
      if (profileChanges.isEmpty()) {
        throw new IllegalStateException("No profile changes found for " + profile.getQpName());
      }
      return profileChanges;
    }
  }

  @NotNull
  private Long getLastAnalysisDate(String componentUuid, DbSession dbSession) {
    return dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, componentUuid)
      .orElseThrow(() -> new IllegalStateException("No snapshot found for " + componentUuid)).getAnalysisDate();
  }

}
