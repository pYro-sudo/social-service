package by.losik.activityservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.shell.command.annotation.CommandScan

@SpringBootApplication
@CommandScan
class ActivityServiceApplication

    fun main(args: Array<String>) {
        runApplication<ActivityServiceApplication>(*args)
}
