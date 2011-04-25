package gpbench

import groovyx.gpars.GParsPool;
import static groovyx.gpars.GParsPool.withPool
import static Region.withTransaction
import org.springframework.transaction.annotation.*

class SaverService {
	static transactional = false 

	void saveCountry(row) {
		Country.withTransaction {
			if (Country.countByGeoWorldMapId(row.CountryId) > 0) return; 
			
			def c = new Country()
			c.geoWorldMapId = row.CountryId.toInteger()
			c.code = row.Country
			c.fips104 = (row.FIPS104 == '--' ) ? row.FIPS104 : null

			c.save();
		}
	}
	
	void saveRegion(row) {
		if( Region.countByGeoWorldMapId(row.RegionID) > 0 ) return;
		Region.withTransaction {
			def country = Country.findByGeoWorldMapId(row.CountryID,[cache:true]);
			def r = new Region()
			r.country = country
			r.code = row.Region
			r.shortCode = row.Code
			r.geoWorldMapId = row.RegionID.toInteger()
			r.save()
		}
	}
	
	void saveCity(row) {
		City.withTransaction {
			//if( City.countByGeoWorldMapId(row.CityId)  > 0 ) return;
			
			Region r = Region.findByGeoWorldMapId(row.RegionID,[cache:true])
			
			City c = new City()
			c.geoWorldMapId = row.CityId.toInteger()
			c.code = row.City
			c.shortCode = row.Code
			c.region = r
			c.latitude = row.Latitude.toFloat()
			c.longitude = row.Longitude.toFloat()
			c.save()
		}
	}


}
