
class BootStrap {

	def loaderService
	
    def init = { servletContext ->
		
		loaderService.with{
			truncateTables()
			loadAllFiles("load_GPars_batched_transactions_per_thread")
			//loaderService.loadAllFiles("load_SimpleJdbcInsert")
			//loaderService.loadAllFiles("load_Gpars_BatchTrans")
			//loaderService.loadAllFiles("load_BatchTrans_Constructor_Properties")

			//loaderService.loadAllFiles("load_Gpars_NoBatch")
			//loaderService.loadAllFiles("load_BatchTrans")
			//loaderService.loadAllFiles("load_NoBatch")
		}
		
    }
    def destroy = {
    }
}
