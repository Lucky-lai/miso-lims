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
@Table(name = "LibraryDerivedInfo")
@Synchronize("Library")
public class LibraryDerivedInfo {

  @Id
  private Long libraryId;

  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;

  public Long getId() {
    return libraryId;
  }

  public Date getLastModified() {
    return lastModified;
  }
}
