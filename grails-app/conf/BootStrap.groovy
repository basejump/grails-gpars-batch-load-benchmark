
class BootStrap {

	def loaderService
	
    def init = { servletContext ->
		
		loaderService.with{
			truncateTables()
			//loadAllFiles("load_GPars_batched_transactions_per_thread")
			loadAllFiles("load_GPars_SimpleJdbcInsert")
			
		}
		
    }
    def destroy = {
    }
}
