package com.fortnite.pronos.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbTableInfoDto {

  private String tableName;
  private String entityName;
  private long rowCount;
  private String sizeDescription;
}
