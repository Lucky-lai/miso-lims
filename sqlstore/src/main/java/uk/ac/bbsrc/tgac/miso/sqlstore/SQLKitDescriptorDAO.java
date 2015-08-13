package uk.ac.bbsrc.tgac.miso.sqlstore;


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import uk.ac.bbsrc.tgac.miso.core.data.KitDescriptor;
import uk.ac.bbsrc.tgac.miso.core.data.impl.kit.KitDescriptorImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.KitType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.store.KitDescriptorStore;
import uk.ac.bbsrc.tgac.miso.core.store.NoteStore;
import uk.ac.bbsrc.tgac.miso.sqlstore.cache.CacheAwareRowMapper;
import uk.ac.bbsrc.tgac.miso.sqlstore.util.DbUtils;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * @author Michal Zak
 */


public class SQLKitDescriptorDAO implements KitDescriptorStore {
    private static final String TABLE_NAME = "KitDescriptor";


    public static final String KIT_DESCRIPTORS_SELECT =
            "SELECT kitDescriptorId, name, version, manufacturer, partNumber, kitType, platformType, units, kitValue " +
                    "FROM KitDescriptor";

    public static final String KIT_DESCRIPTOR_SELECT_BY_ID =
            KIT_DESCRIPTORS_SELECT + " WHERE kitDescriptorId=?";

    public static final String KIT_DESCRIPTOR_SELECT_BY_MANUFACTURER =
            KIT_DESCRIPTORS_SELECT + " WHERE manufacturer =?";

    public static final String KIT_DESCRIPTOR_SELECT_BY_PART_NUMBER =
            KIT_DESCRIPTORS_SELECT + " WHERE partNumber=?";

    public static final String KIT_DESCRIPTOR_SELECT_BY_TYPE =
            KIT_DESCRIPTORS_SELECT + " WHERE kitType=?";

    public static final String KIT_DESCRIPTOR_SELECT_BY_PLATFORM =
            KIT_DESCRIPTORS_SELECT + " WHERE platformType=?";



    public static final String KIT_DESCRIPTOR_SELECT_BY_UNITS =
            KIT_DESCRIPTORS_SELECT + " WHERE units=?";

    public static final String KIT_DESCRIPTOR_UPDATE =
            "UPDATE KitDescriptor " +
                    "SET name=:name, version=:version, manufacturer=:manufacturer, partNumber=:partNumber, kitType=:kitType, platformType=:platformType, units=:units, kitValue=:kitValue " +
                    "WHERE kitDescriptorId=:kitDescriptorId";


    protected static final Logger log = LoggerFactory.getLogger(SQLKitDescriptorDAO.class);
    private JdbcTemplate template;
    private NoteStore noteDAO;
    private CascadeType cascadeType;



    public Collection<KitDescriptor> listAll() throws IOException {
        return template.query(KIT_DESCRIPTORS_SELECT, new KitDescriptorMapper());
    }

    @Override
    public int count() throws IOException {
        return template.queryForInt("SELECT count(*) FROM " + TABLE_NAME);
    }


    @Autowired
    private CacheManager cacheManager;

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Autowired
    private DataObjectFactory dataObjectFactory;

    public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
        this.dataObjectFactory = dataObjectFactory;
    }

    public void setNoteDAO(NoteStore noteDAO) {
        this.noteDAO = noteDAO;
    }

    public JdbcTemplate getJdbcTemplate() {
        return template;
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public void setCascadeType(CascadeType cascadeType) {
        this.cascadeType = cascadeType;
    }

    public KitDescriptor getKitDescriptorById(long id) throws IOException {
        List eResults = template.query(KIT_DESCRIPTOR_SELECT_BY_ID, new Object[]{id}, new KitDescriptorMapper());
        return eResults.size() > 0 ? (KitDescriptor) eResults.get(0) : null;
    }

    public KitDescriptor getKitDescriptorByPartNumber(String partNumber) throws IOException {
        List eResults = template.query(KIT_DESCRIPTOR_SELECT_BY_PART_NUMBER, new Object[]{partNumber}, new KitDescriptorMapper());
        return eResults.size() > 0 ? (KitDescriptor) eResults.get(0) : null;
    }

    public List<KitDescriptor> listKitDescriptorsByManufacturer(String manufacturer) throws IOException {
        return template.query(KIT_DESCRIPTOR_SELECT_BY_MANUFACTURER, new Object[]{manufacturer}, new KitDescriptorMapper());
    }

    public List<KitDescriptor> listKitDescriptorsByType(KitType kitType) throws IOException {
        return template.query(KIT_DESCRIPTOR_SELECT_BY_TYPE, new Object[]{kitType.getKey()}, new KitDescriptorMapper());
    }


    public List<KitDescriptor> listKitDescriptorsByPlatform(PlatformType platformType) throws IOException {
        return template.query(KIT_DESCRIPTOR_SELECT_BY_PLATFORM, new Object[]{platformType.getKey()}, new KitDescriptorMapper());
    }
    public List<KitDescriptor> listKitDescriptorsByUnits(String units) throws IOException {
        return template.query(KIT_DESCRIPTOR_SELECT_BY_UNITS, new Object[]{units}, new KitDescriptorMapper());
    }

    public KitDescriptor get(long id) throws IOException {
        List eResults = template.query(KIT_DESCRIPTOR_SELECT_BY_ID, new Object[]{id}, new KitDescriptorMapper());
        return eResults.size() > 0 ? (KitDescriptor) eResults.get(0) : null;
    }

    @Override
    public KitDescriptor lazyGet(long id) throws IOException {
        return get(id);
    }

    public long save(KitDescriptor kd) throws IOException {
        //log.info("Saving " + kd.toString() + " : " + kd.getKitType() + " : " + kd.getPlatformType());
        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue("name", kd.getName())
                .addValue("version", kd.getVersion())
                .addValue("manufacturer", kd.getManufacturer())
                .addValue("partNumber", kd.getPartNumber())
                .addValue("kitType", kd.getKitType().getKey())
                .addValue("platformType", kd.getPlatformType().getKey())
                .addValue("units", kd.getUnits())
                .addValue("kitValue", kd.getKitValue());

        if (kd.getKitDescriptorId() == KitDescriptorImpl.UNSAVED_ID) {
            SimpleJdbcInsert insert = new SimpleJdbcInsert(template)
                    .withTableName("KitDescriptor")
                    .usingGeneratedKeyColumns("kitDescriptorId");
            Number newId = insert.executeAndReturnKey(params);
            kd.setKitDescriptorId(newId.longValue());
        }
        else {
            params.addValue("kitDescriptorId", kd.getKitDescriptorId());
            NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
            namedTemplate.update(KIT_DESCRIPTOR_UPDATE, params);
        }

        return kd.getKitDescriptorId();
    }




    public class KitDescriptorMapper implements RowMapper<KitDescriptor> {


        public KitDescriptor mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("kitDescriptorId");



            /*if (isCacheEnabled() && lookupCache(cacheManager) != null) {
                Element element;
                if ((element = lookupCache(cacheManager).get(DbUtils.hashCodeCacheKeyFor(id))) != null) {
                    log.debug("Cache hit on map for KitDescriptor " + id);
                    return (KitDescriptor)element.getObjectValue();
                }
            }*/

            KitDescriptor kitDescriptor = new KitDescriptorImpl();


                //kitDescriptor = getKitDescriptorById(id);


                kitDescriptor.setKitDescriptorId(id);
                kitDescriptor.setName(rs.getString("name"));
                kitDescriptor.setVersion(rs.getDouble("version"));
                kitDescriptor.setManufacturer(rs.getString("manufacturer"));
                kitDescriptor.setPartNumber(rs.getString("partNumber"));
                kitDescriptor.setKitType(KitType.get(rs.getString("kitType")));
                kitDescriptor.setPlatformType(PlatformType.get(rs.getString("platformType")));
                kitDescriptor.setUnits(rs.getString("units"));
                kitDescriptor.setKitValue(rs.getFloat("kitValue"));

            return kitDescriptor;
        }
    }
}
