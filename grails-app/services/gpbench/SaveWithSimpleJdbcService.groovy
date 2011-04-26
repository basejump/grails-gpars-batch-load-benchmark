package gpbench

import groovyx.gpars.GParsPool;
import static groovyx.gpars.GParsPool.withPool
import static Region.withTransaction
import org.springframework.transaction.annotation.*
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


class SaveWithSimpleJdbcService {
	static transactional = true 

	def dataSource
	
	def load_SimpleJdbcInsert(name,csvFile=null) {	
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		
		def sjt = new SimpleJdbcTemplate(dataSource)
	
		reader.each{ row ->
			"save$name"(row,sjt)
		}
	}
	
	def loadList(name,list) {	
		def sjt = new SimpleJdbcTemplate(dataSource)
		list.each{ map ->
			"save$name"(map,sjt)
		}
	}
	
	void saveRegion(row,simpleJdbcTemplate) {
		//def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
		//def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from region where geo_world_map_id = ?", row.RegionID.toInteger())
		//if( countByGeoWorldMapId > 0 ) return
		
		//def countryId = jdbcTemplate.queryForInt("Select id from country where geo_world_map_id = ?",row.CountryID.toInteger()) 
/*		def valmap = [
			country_id: 1,
			code: row.Region,
			short_code: row.Code,
			geo_world_map_id: row.RegionID.toInteger(),
			version:0
		]
		simpleJdbcInsert.execute(new MapSqlParameterSource(valmap))*/
		def countryId = simpleJdbcTemplate.queryForInt("Select id from country where geo_world_map_id = ?",row.CountryID.toInteger()) 
		String query = "INSERT INTO region (country_id, code, short_code,geo_world_map_id,version) VALUES (?,?,?,?,?)";
		simpleJdbcTemplate.update(query,countryId,row.Region,row.Code,row.RegionID.toInteger(),0)
	}
	

	void saveCity(row,simpleJdbcTemplate) {
/*		def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
		def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from city where geo_world_map_id = ?", row.CityId.toInteger())
		if( countByGeoWorldMapId > 0 ) return */
		
		//def regionId = jdbcTemplate.queryForInt("Select id from region where geo_world_map_id = ?",row.RegionID.toInteger()) 
/*		def valmap = [
			code: row.City,
			short_code: row.Code,
			geo_world_map_id: row.CityId.toInteger(),
			region_id: 1,
			latitude: row.Latitude.toFloat(),
			longitude: row.Longitude.toFloat(),
			version:0
		]
		
		simpleJdbcInsert.execute(valmap)*/
		def regionId = simpleJdbcTemplate.queryForInt("Select id from region where geo_world_map_id = ?",row.RegionID.toInteger()) 
		String query = "INSERT INTO city (code, short_code, geo_world_map_id,region_id,latitude,longitude,version) VALUES (?,?,?,?,?,?,?)";
		simpleJdbcTemplate.update(query,row.City,row.Code,row.CityId.toInteger(),regionId,row.Latitude.toFloat(),row.Longitude.toFloat(),0)
	}

	void saveCountry(row,simpleJdbcTemplate) {
/*		def jdbcTemplate = simpleJdbcInsert.jdbcTemplate
		def countByGeoWorldMapId = jdbcTemplate.queryForInt("Select count(1) from country where geo_world_map_id = ?", row.CountryId.toInteger())
		if( countByGeoWorldMapId > 0 ) return
		
		def valmap = [
			fips104: row.FIPS104,
			code: row.Country,
			geo_world_map_id: row.CountryId.toInteger(),
			version:0
		]
		simpleJdbcInsert.execute(valmap)*/
		
		String query = "INSERT INTO country (fips104, code, geo_world_map_id,version,id) VALUES (?,?,?,?,?)";
		simpleJdbcTemplate.update(query,row.FIPS104,row.Country,row.CountryId.toInteger(),0,row.CountryId)
	}

}
