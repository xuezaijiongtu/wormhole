/*-
 * <<
 * wormhole
 * ==
 * Copyright (C) 2016 - 2017 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */


package edp.rider.rest.util

import edp.rider.common.RiderLogger
import edp.rider.rest.persistence.entities.{Instance, Namespace, NamespaceInfo, NsDatabase}
import edp.rider.rest.util.CommonUtils._

object NamespaceUtils extends RiderLogger {

  def generateStandardNs(ns: NamespaceInfo) = Seq(ns.nsSys, ns.nsInstance, ns.nsDatabase, ns.nsTable, ns.nsVersion, ns.nsDbpar, ns.nsTablepar).mkString(".")

  def generateStandardNs(ns: Namespace) = Seq(ns.nsSys, ns.nsInstance, ns.nsDatabase, ns.nsTable, ns.nsVersion, ns.nsDbpar, ns.nsTablepar).mkString(".")

  def getConnUrl(instance: Instance, db: NsDatabase, connType: String = "sink") = {
    instance.nsSys match {
      case "mysql" | "postgresql" | "phoenix" =>
        db.config match {
          case Some(conf) =>
            if (conf != "") {
              val confStr =
              //                if (isJson(conf))
              //                  JSON.parseObject(conf).keySet().toArray.map(key => s"$key=${JSON.parseObject(conf).get(key).toString}").mkString("&")
              //                else
                (keyEqualValuePattern.toString.r findAllIn conf.split(",").mkString("&")).toList.mkString("&")
              s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}?$confStr"
            } else s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}"
          case None => s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}"
        }
      case "oracle" =>
        val hostPort = instance.connUrl.split(":")
        val serviceName = db.config match {
          case Some(conf) =>
            if (conf != "") {
              //              if (isJson(conf)) {
              //                if (JSON.parseObject(conf).containsKey("service_name"))
              //                  JSON.parseObject(conf).getString("service_name")
              //                else if (JSON.parseObject(conf).containsKey("SERVICE_NAME"))
              //                  JSON.parseObject(conf).getString("SERVICE_NAME")
              //                else ""
              //              } else {
              if (conf.indexOf("service_name") >= 0) {
                val index = conf.indexOf("service_name")
                val length = "service_name".length
                val lastPart = conf.indexOf(",", index + length)
                val endIndex = if (lastPart < 0) conf.length else lastPart
                conf.substring(conf.indexOf("=", index + length) + 1, endIndex)
              } else if (conf.indexOf("SERVICE_NAME") >= 0) {
                val index = conf.indexOf("SERVICE_NAME")
                val length = "SERVICE_NAME".length
                val lastPart = conf.indexOf(",", index + length)
                val endIndex = if (lastPart < 0) conf.length else lastPart
                conf.substring(conf.indexOf("=", index + length) + 1, endIndex)
              } else {
                riderLogger.info("NO ORACLE SERVICE NAME:")
                ""
              }
              //              }
            } else ""
          case None => ""
        }
        s"jdbc:oracle:thin:@(DESCRIPTION=(FAILOVER = yes)(ADDRESS = (PROTOCOL = TCP)(HOST =${hostPort(0)})(PORT = ${hostPort(1)}))(CONNECT_DATA =(SERVER = DEDICATED)(SERVICE_NAME = $serviceName)))"

      case "cassandra" =>
        if (connType == "lookup")
          db.config match {
            case Some(conf) =>
              if (conf != "") {
                val confStr =
                //                  if (isJson(conf))
                //                    JSON.parseObject(conf).keySet().toArray.map(key => s"$key=${JSON.parseObject(conf).get(key).toString}").mkString("&")
                //                  else
                  (keyEqualValuePattern.toString.r findAllIn conf.split(",").mkString("&")).toList.mkString("&")
                s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}?$confStr"
              } else s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}"
            case None => s"jdbc:${instance.nsSys}://${instance.connUrl}/${db.nsDatabase}"
          }
        else instance.connUrl
      case "es" =>
        if (connType == "lookup") {
          if (db.config.nonEmpty && db.config.get != "") {
            val confArray = (keyEqualValuePattern.toString.r findAllIn db.config.get.split(",").mkString("&")).toList
            val connConf = confArray.filter(_.contains("cluster.name")).headOption
            if (connConf.nonEmpty)
              s"jdbc:sql4es://${instance.connUrl}/${db.nsDatabase}?${connConf.get}"
            else s"jdbc:sql4es://${instance.connUrl}/${db.nsDatabase}"
          } else s"jdbc:sql4es://${instance.connUrl}/${db.nsDatabase}"
        } else instance.connUrl
      case _ => instance.connUrl
    }

  }

}
