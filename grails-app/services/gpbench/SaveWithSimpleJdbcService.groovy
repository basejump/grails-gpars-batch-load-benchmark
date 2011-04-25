package gpbench

import groovyx.gpars.GParsPool;
import static groovyx.gpars.GParsPool.withPool
import static Region.withTransaction
import org.springframework.transaction.annotation.*
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource


class SaveWithSimpleJdbcService {
	static transactional = true 

	
	void saveRegion(row,simpleJdbcInsert) {
		def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
		def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from region where geo_world_map_id = ?", row.RegionID.toInteger())
		if( countByGeoWorldMapId > 0 ) return
		
		def countryId = jdbcTemplate.queryForInt("Select id from country where geo_world_map_id = ?",row.CountryID.toInteger()) 
		def valmap = [
			country_id: countryId,
			code: row.Region,
			short_code: row.Code,
			geo_world_map_id: row.RegionID.toInteger(),
			version:0
		]
		simpleJdbcInsert.execute(new MapSqlParameterSource(valmap))
	}
	

	void saveCity(row,simpleJdbcInsert) {
		def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
/*		def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from city where geo_world_map_id = ?", row.CityId.toInteger())
		if( countByGeoWorldMapId > 0 ) return*/
		
		def regionId = jdbcTemplate.queryForInt("Select id from region where geo_world_map_id = ?",row.RegionID.toInteger()) 
		def valmap = [
			code: row.City,
			short_code: row.Code,
			geo_world_map_id: row.CityId.toInteger(),
			region_id: regionId,
			latitude: row.Latitude.toFloat(),
			longitude: row.Longitude.toFloat(),
			version:0
		]
		
		simpleJdbcInsert.execute(new MapSqlParameterSource(valmap))
	}

	void saveCountry(row,simpleJdbcInsert) {
		def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
		def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from country where geo_world_map_id = ?", row.CountryId.toInteger())
		if( countByGeoWorldMapId > 0 ) return
		
		def valmap = [
			fips104: row.FIPS104,
			code: row.Country,
			geo_world_map_id: row.CountryId.toInteger(),
			version:0
		]
		simpleJdbcInsert.execute(valmap)
	}

}
