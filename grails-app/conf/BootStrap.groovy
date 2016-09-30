import gpbench.LoaderService

class BootStrap {

	LoaderService loaderService

	def init = { servletContext ->

		loaderService.with {
			truncateTables()
			runBenchMark()
		}

	}

}
