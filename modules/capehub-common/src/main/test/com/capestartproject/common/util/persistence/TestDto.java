package com.capestartproject.common.util.persistence;

import com.capestartproject.common.util.data.Function;

import org.junit.Ignore;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity(name = "Test")
@Table(name = "test")
@NamedQueries(@NamedQuery(name = "Test.findAll", query = "select a from Test a"))
@Ignore
public class TestDto {
  @Id
  @GeneratedValue
  private long id;

  @Column(name = "key", length = 128, nullable = false)
  private String key;

  @Column(name = "value", length = 128, nullable = false)
  private String value;

  public static TestDto create(String key, String value) {
    final TestDto dto = new TestDto();
    dto.key = key;
    dto.value = value;
    return dto;
  }

  public long getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public static final Function<EntityManager, List<TestDto>> findAll = Queries.named.findAll("Test.findAll");
}
