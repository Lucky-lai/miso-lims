package uk.ac.bbsrc.tgac.miso.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;

import uk.ac.bbsrc.tgac.miso.core.data.impl.BoxDerivedInfo;
import uk.ac.bbsrc.tgac.miso.core.data.impl.UserImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.changelog.BoxChangeLog;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;
import uk.ac.bbsrc.tgac.miso.core.util.CoverageIgnore;

@MappedSuperclass
public abstract class AbstractBox implements Box {

  public static final Long UNSAVED_ID = 0L;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "securityProfile_profileId")
  private SecurityProfile securityProfile;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long boxId = UNSAVED_ID;
  private String name;
  private String alias;
  private String description;
  private String identificationBarcode;
  private String locationBarcode;

  @ManyToOne(targetEntity = UserImpl.class)
  @JoinColumn(name = "lastModifier", nullable = false)
  private User lastModifier;

  @OneToOne
  @PrimaryKeyJoinColumn
  private BoxDerivedInfo derivedInfo;

  @ManyToOne
  @JoinColumn(name = "boxSizeId")
  private BoxSize size;
  @ManyToOne
  @JoinColumn(name = "boxUseId")
  private BoxUse use;

  @OneToMany(targetEntity = BoxChangeLog.class, mappedBy = "box")
  private final Collection<ChangeLog> changeLog;

  @CoverageIgnore
  public AbstractBox() {
    securityProfile = null;
    boxId = AbstractBox.UNSAVED_ID;
    changeLog = new ArrayList<>();
  }

  @Override
  public User getLastModifier() {
    return lastModifier;
  }

  @Override
  public void setLastModifier(User lastModifier) {
    this.lastModifier = lastModifier;
  }

  @Override
  public long getId() {
    return boxId;
  }

  @Override
  public void setId(long id) {
    this.boxId = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getAlias() {
    return alias;
  }

  @Override
  public void setAlias(String alias) {
    this.alias = alias;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getIdentificationBarcode() {
    return identificationBarcode;
  }

  @Override
  public void setIdentificationBarcode(String identificationBarcode) {
    this.identificationBarcode = identificationBarcode;
  }

  @Override
  public String getLocationBarcode() {
    return locationBarcode;
  }

  @Override
  public void setLocationBarcode(String locationBarcode) {
    this.locationBarcode = locationBarcode;
  }

  @Override
  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  @Override
  public void setSecurityProfile(SecurityProfile securityProfile) {
    this.securityProfile = securityProfile;
  }

  @CoverageIgnore
  @Override
  public void inheritPermissions(SecurableByProfile parent) throws SecurityException {
    if (parent.getSecurityProfile().getOwner() != null) {
      setSecurityProfile(parent.getSecurityProfile());
    } else {
      throw new SecurityException("Cannot inherit permissions when parent object owner is not set!");
    }
  }

  @CoverageIgnore
  @Override
  public boolean userCanRead(User user) {
    return securityProfile.userCanRead(user);
  }

  @CoverageIgnore
  @Override
  public boolean userCanWrite(User user) {
    return securityProfile.userCanWrite(user);
  }

  @Override
  public BoxUse getUse() {
    return use;
  }

  @Override
  public void setUse(BoxUse use) {
    this.use = use;
  }

  @Override
  public BoxSize getSize() {
    return size;
  }

  @Override
  public void setSize(BoxSize size) {
    this.size = size;
  }

  @Override
  public Collection<ChangeLog> getChangeLog() {
    return changeLog;
  }

  @Override
  public Date getLastModified() {
    return (derivedInfo == null ? null : derivedInfo.getLastModified());
  }

}
