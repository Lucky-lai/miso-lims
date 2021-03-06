package uk.ac.bbsrc.tgac.miso.persistence.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.store.LibraryDilutionStore;
import uk.ac.bbsrc.tgac.miso.sqlstore.util.DbUtils;

@Repository
@Transactional(rollbackFor = Exception.class)
public class HibernateLibraryDilutionDao implements LibraryDilutionStore {

  @Autowired
  private SessionFactory sessionFactory;

  private Session currentSession() {
    return getSessionFactory().getCurrentSession();
  }

  private void addSearchRestrictions(Criteria criteria, String querystr) {
    criteria.createAlias("library", "library");
    criteria
        .add(DbUtils.searchRestrictions(querystr, "name", "identificationBarcode", "library.name", "library.alias", "library.description"));
  }

  @Override
  public long save(LibraryDilution dilution) throws IOException {
    long id;
    if (dilution.getId() == LibraryDilution.UNSAVED_ID) {
      id = (long) currentSession().save(dilution);
    } else {
      currentSession().update(dilution);
      id = dilution.getId();
    }
    return id;
  }

  @Override
  public LibraryDilution get(long id) throws IOException {
    return (LibraryDilution) currentSession().get(LibraryDilution.class, id);
  }

  @Override
  public Collection<LibraryDilution> listAll() throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public boolean remove(LibraryDilution dilution) throws IOException {
    if (dilution.isDeletable()) {
      currentSession().delete(dilution);
      LibraryDilution testIfExists = get(dilution.getId());

      return testIfExists == null;
    } else {
      return false;
    }
  }

  @Override
  public int count() throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    return ((Long) criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
  }

  @Override
  public int countByPlatform(PlatformType platform) throws IOException {
    if (platform == null) throw new NullPointerException("Cannot look up library dilutions by null platform");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.createAlias("library", "library");
    criteria.add(Restrictions.eq("library.platformType", platform));
    return ((Long) criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
  }

  @Override
  public Integer countAllBySearchAndPlatform(String search, PlatformType platform) throws IOException {
    if (platform == null) throw new NullPointerException("Cannot look up library dilutions by null platform");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    addSearchRestrictions(criteria, search);
    criteria.add(Restrictions.eq("library.platformType", platform));
    return ((Long) criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
  }

  @Override
  public Collection<LibraryDilution> listByLibraryId(long libraryId) throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.add(Restrictions.eq("library.id", libraryId));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public Collection<LibraryDilution> listAllLibraryDilutionsByPlatform(PlatformType platformType) throws IOException {
    if (platformType == null) throw new NullPointerException("Cannot look up library dilutions by null platform");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.createAlias("library", "library");
    criteria.add(Restrictions.eq("library.platformType", platformType));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public Collection<LibraryDilution> listAllLibraryDilutionsByProjectId(long projectId) throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.createAlias("library", "library");
    criteria.createAlias("library.sample", "sample");
    criteria.createAlias("sample.project", "project");
    criteria.add(Restrictions.eq("project.id", projectId));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public Collection<LibraryDilution> listAllLibraryDilutionsBySearchAndPlatform(String query, PlatformType platformType)
      throws IOException {
    if (platformType == null) throw new NullPointerException("Cannot look up library dilutions by null platform");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    addSearchRestrictions(criteria, query);
    criteria.add(Restrictions.eq("library.platformType", platformType));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public Collection<LibraryDilution> listAllLibraryDilutionsBySearchOnly(String query) throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    addSearchRestrictions(criteria, query);
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public Collection<LibraryDilution> listAllLibraryDilutionsByProjectAndPlatform(long projectId, PlatformType platformType)
      throws IOException {
    if (platformType == null) throw new NullPointerException("Cannot look up library dilutions by null platform");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.createAlias("library", "library").createAlias("library.sample", "sample").createAlias("sample.project", "project");
    criteria.add(Restrictions.and(Restrictions.eq("project.id", projectId), Restrictions.eq("library.platformType", platformType)));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public LibraryDilution getLibraryDilutionByBarcode(String barcode) throws IOException {
    if (barcode == null) throw new IOException("Barcode cannot be null!");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.add(Restrictions.eq("identificationBarcode", barcode));
    return (LibraryDilution) criteria.uniqueResult();
  }

  @Override
  public Collection<LibraryDilution> listAllWithLimit(long limit) throws IOException {
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    criteria.setMaxResults((int) limit);
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = criteria.list();
    return records;
  }

  @Override
  public List<LibraryDilution> listBySearchOffsetAndNumResultsAndPlatform(int offset, int limit, String querystr, String sortDir,
      String sortCol, PlatformType platform) throws IOException {
    if (offset < 0 || limit < 0) throw new IOException("Limit and Offset must not be less than zero");
    Criteria criteria = currentSession().createCriteria(LibraryDilution.class);
    addSearchRestrictions(criteria, querystr);
    criteria.setFirstResult(offset);
    criteria.setMaxResults(limit);
    criteria.addOrder("asc".equalsIgnoreCase(sortDir) ? Order.asc(sortCol) : Order.desc(sortCol));
    criteria.setProjection(Projections.property("id"));
    @SuppressWarnings("unchecked")
    List<Long> ids = criteria.list();
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    // We do this in two steps to make a smaller query that that the database can optimise
    Criteria query = currentSession().createCriteria(LibraryDilution.class);
    query.add(Restrictions.in("id", ids));
    query.addOrder("asc".equalsIgnoreCase(sortDir) ? Order.asc(sortCol) : Order.desc(sortCol));
    @SuppressWarnings("unchecked")
    List<LibraryDilution> records = query.list();
    return records;
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

}
