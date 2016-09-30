package gpbench

import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

@Transactional
class RegionService {

	@GrailsCompileStatic
	public void insertWithDataBinding(Map row) {
		//println row
		Region r = new Region(row)
		r.id = row.id as Long
		r.save(failOnError: true)
	}

	@GrailsCompileStatic
	public void insertWithSetter(Map row) {
		Country country = Country.load(row['country']['id'] as Long);
		Region r = new Region()
		r.id = row.id as Long
		DomainUtils.copyDomain(r, row)
		r.country = country
		r.save(failOnError: true)
	}


}
