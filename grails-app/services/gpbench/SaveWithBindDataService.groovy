package gpbench

import groovyx.gpars.GParsPool;
import static groovyx.gpars.GParsPool.withPool
import static Region.withTransaction
import org.springframework.transaction.annotation.*

class SaveWithBindDataService {
	static transactional = false 

	void saveRegion(row) {
		if( Region.countByGeoWorldMapId(row.RegionID) > 0 ) return;
		Region.withTransaction {
			def country = Country.findByGeoWorldMapId(row.CountryID,[cache:true]);
			def r = new Region(
				country: country,
				code: row.Region,
				shortCode: row.Code,
				geoWorldMapId: row.RegionID
			)
			r.save()
		}
	}
	
	//Transactional save. if one doesn't exist then create it, otherwise participate
	void saveCity(row) {
		City.withTransaction {
			//if( City.countByGeoWorldMapId(row.CityId)  > 0 ) return;
			
			Region r = Region.findByGeoWorldMapId(row.RegionID,[cache:true])
			
			def props = [
				geoWorldMapId: row.CityId.toInteger(),
				code: row.City,
				shortCode: row.Code,
				region: r,
				latitude: row.Latitude.toFloat(),
				longitude: row.Longitude.toFloat()
			]
			City c = new City(props)
			c.save()
		}
	}

	//Transactional save. if one doesn't exist then create it, otherwise participate
	void saveCountry(row) {
		Country.withTransaction {
			if (Country.countByGeoWorldMapId(row.CountryId) > 0) return;
			// Fips 104 exists?
			if( row.FIPS104.equals('--')){
				row.FIPS104 == null;
			}
			def c = new Country(
				geoWorldMapId: row.CountryId,
				code: row.Country,
				fips104: row.FIPS104)

			c.save();
		}
	}

}
