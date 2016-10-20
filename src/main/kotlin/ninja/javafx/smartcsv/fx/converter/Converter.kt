/*
   The MIT License (MIT)
   -----------------------------------------------------------------------------
   Copyright (c) 2016 javafx.ninja <info@javafx.ninja>
   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:
   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.
   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.
*/
package ninja.javafx.smartcsv.fx.converter

import com.beust.klaxon.*
import java.io.File
import java.util.*

class Converter(val name: String) {

    fun parse(name: String): Any {
        val inputStream = File(name).inputStream()
        return Parser().parse(inputStream)!!
    }

    fun convert() {
        val root = parse(name) as JsonObject
        val headers = root.obj("headers")
        val headerList: JsonArray<String>? = headers?.array("list")

        val columnMap = root.obj("columns")

        val fields = ArrayList<JsonObject>()

        headerList?.forEach {

            val schemaColumn = JsonObject(HashMap<String, Any>())
            schemaColumn["name"] = it

            val column: JsonObject? = columnMap?.obj(it)
            if (column != null) {
                val isInteger = column.boolean("integer") ?: false
                val isNotEmpty = column.boolean("not empty") ?: false
                val isDouble = column.boolean("double") ?: false
                val isUnique = column.boolean("unique") ?: false
                val isAlphanumeric = column.boolean("alphanumeric") ?: false
                val minlength = column.int("minlength")
                val maxlength = column.int("maxlength")
                val date = column.string("date")
                val groovy = column.string("groovy")
                val regexp = column.string("regexp")
                val valueOfList: JsonArray<String>? = column.array("value of")

                schemaColumn["type"] = "string"
                if (isInteger) schemaColumn["type"] = "integer"
                if (isDouble) schemaColumn["type"] = "number"

                date?.let {
                    schemaColumn.put("type", "date")
                    schemaColumn.put("format", date)
                }
                groovy?.let { schemaColumn["groovy"] = groovy }
                constraint(schemaColumn, "unique", isUnique)
                constraint(schemaColumn, "required", isNotEmpty)
                constraint(schemaColumn, "minLength", minlength)
                constraint(schemaColumn, "maxLength", maxlength)
                constraint(schemaColumn, "enum", valueOfList)
                constraint(schemaColumn, "pattern", regexp)
                if (isAlphanumeric) constraint(schemaColumn, "pattern",  "[0-9a-zA-Z]*")
            }
            fields.add(schemaColumn)

        }

        val newName = name.substringBeforeLast('.') + "_json_schema." + name.substringAfterLast('.')
        File(newName).writeText(json { obj(Pair("fields", JsonArray(fields))) }.toJsonString(true))

    }

    fun constraint(column : JsonObject, key : String, value: Any?) {
        if (value == null) return
        val constraints = column.getOrPut("constraints") { JsonObject(mutableMapOf()) } as JsonObject
        constraints[key] = value
    }

}




fun main(args: Array<String>) {
    if (args.size == 1) {
        Converter(args[0]).convert()
    } else {
        println("missing file to convert as argument")
    }
}