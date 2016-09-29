package gpbench

import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

@Transactional
class RegionService {
	JdbcTemplate jdbcTemplate

	public void insertWithDataBinding(Map row) {
		if (Region.countByGeoWorldMapId(row.RegionID) > 0) return;
		Country country = Country.findByGeoWorldMapId(row.CountryID, [cache: true]);

		Map props = [
				country      : [id: country.id],
				code         : row.Region,
				shortCode    : row.Code,
				geoWorldMapId: row.RegionID
		]

		Region r = new Region(props)
		r.save()
	}

	public void insertWithoutDataBinding(Map row) {
		Country country = Country.findByGeoWorldMapId(row.CountryID, [cache: true]);
		Region r = new Region()
		r.country = country
		r.code = row.Region
		r.shortCode = row.Code
		r.geoWorldMapId = row.RegionID as Integer
		r.save()
	}

	public void insertWithSetter(Map row) {
		Country country = Country.findByGeoWorldMapId(row['country.id'], [cache: true]);
		Region r = new Region()
		DomainUtils.copyDomain(r, row)
		r.country = country
		r.save()
	}


	public void insertWithJdbc(row) {
		def countryId = jdbcTemplate.queryForInt("Select id from country where geo_world_map_id = ?", row.CountryID.toInteger())
		String query = "INSERT INTO region (country_id, code, short_code, geo_world_map_id, version) VALUES (?,?,?,?,?)";
		jdbcTemplate.update(query, countryId, row.Region, row.Code, row.RegionID.toInteger(), 0)
	}

	public void insetToTestTable(row) {
		def countryId = jdbcTemplate.queryForInt("Select id from country_test where geoWorldMapId = ?", row.CountryID.toInteger())
		String query = "INSERT INTO region_test (`country.id`, code, shortCode, geoWorldMapId, version) VALUES (?,?,?,?,?)";
		jdbcTemplate.update(query, countryId, row.Region, row.Code, row.RegionID.toInteger(), 0)
	}

}
