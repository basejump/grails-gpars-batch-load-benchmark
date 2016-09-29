package gpbench

import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

@Transactional
class CityService {

	JdbcTemplate jdbcTemplate

	public void insertWithDataBinding(Map row) {
		Region r = Region.findByGeoWorldMapId(row.RegionID, [cache: true])

		def props = [
				geoWorldMapId: row.CityId,
				code         : row.City,
				shortCode    : row.Code,
				"region.id"  : r.id,
				latitude     : row.Latitude,
				longitude    : row.Longitude
		]
		City c = new City(props)
		c.save()
	}

	public void insertWithoutDataBinding(row) {
		Region r = Region.findByGeoWorldMapId(row.RegionID, [cache: true])

		City c = new City()
		c.geoWorldMapId = row.CityId.toInteger()
		c.code = row.City
		c.shortCode = row.Code
		c.region = r
		c.latitude = row.Latitude.toFloat()
		c.longitude = row.Longitude.toFloat()
		c.save()
	}

	public void insertWithSetter(Map row) {
		Region r = Region.findByGeoWorldMapId(row['region.id'], [cache: true])
		City c = new City()
		DomainUtils.copyDomain(c, row)
		c.region = r
		c.save()
	}


	void insertWithJdbc(row) {
		def regionId = jdbcTemplate.queryForInt("Select id from region where geo_world_map_id = ?", row.RegionID.toInteger())
		String query = "INSERT INTO city (code, short_code, geo_world_map_id,region_id,latitude,longitude,version) VALUES (?,?,?,?,?,?,?)";
		jdbcTemplate.update(query, row.City, row.Code, row.CityId.toInteger(), regionId, row.Latitude.toFloat(), row.Longitude.toFloat(), 0)
	}

	void insetToTestTable(row) {
		def regionId = jdbcTemplate.queryForInt("Select id from region_test where geoWorldMapId = ?", row.RegionID.toInteger())
		String query = "INSERT INTO city_test (code, shortCode, geoWorldMapId, `region.id`, latitude,longitude,version) VALUES (?,?,?,?,?,?,?)";
		jdbcTemplate.update(query, row.City, row.Code, row.CityId.toInteger(), regionId, row.Latitude.toFloat(), row.Longitude.toFloat(), 0)
	}

}
