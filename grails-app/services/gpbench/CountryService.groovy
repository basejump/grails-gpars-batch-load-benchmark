package gpbench

import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

@Transactional
class CountryService {

	JdbcTemplate jdbcTemplate

	public void insertWithDataBinding(Map row) {
		if (Country.countByGeoWorldMapId(row.CountryId) > 0) return;

		if (row.FIPS104.equals('--')) row.FIPS104 == null;

		Map props = [
				geoWorldMapId: row.CountryId,
				code         : row.Country,
				fips104      : row.FIPS104
		]

		Country c = new Country(props)
		c.save()
	}

	public void insertWithoutDataBinding(row) {
		if (Country.countByGeoWorldMapId(row.CountryId) > 0) return

		Country c = new Country()
		c.geoWorldMapId = row.CountryId.toInteger()
		c.code = row.Country
		c.fips104 = (row.FIPS104 == '--') ? row.FIPS104 : null

		c.save()
	}

	public void insertWithSetter(Map row) {
		Country c = new Country()
		DomainUtils.copyDomain(c, row)
		c.save()
	}


	void insertWithJdbc(row) {
		String query = "INSERT INTO country (fips104, code, geo_world_map_id,version) VALUES (?,?,?,?)";
		jdbcTemplate.update(query, row.FIPS104, row.Country, row.CountryId.toInteger(), 0)
	}

	void insetToTestTable(row) {
		String query = "INSERT INTO country_test (fips104, code, geoWorldMapId,version) VALUES (?,?,?,?)";
		jdbcTemplate.update(query, row.FIPS104, row.Country, row.CountryId.toInteger(), 0)
	}

}
