package db

import java.sql.Connection

/**
 * Created by tauraamui on 27/10/2016.
 */

abstract class GenericDAO {

    var connection: Connection? = null
    var tableName: String = ""

    abstract fun count() {

    }
}