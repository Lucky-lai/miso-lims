package uk.ac.bbsrc.tgac.miso.core.data.impl;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Synchronize;

@Entity
@Immutable
@Table(name = "SampleDerivedInfo")
@Synchronize("Sample")
public class SampleDerivedInfo {

  @Id
  private Long sampleId;

  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;

  public Long getId() {
    return sampleId;
  }

  public Date getLastModified() {
    return lastModified;
  }
}
