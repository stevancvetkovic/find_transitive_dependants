<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Incremental Maven Release Plugin
============

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Build Status](https://travis-ci.org/stevancvetkovic/incremental-maven-release-plugin.svg?branch=master)][build]

This plugin adds ability to manage Maven multi-module projects in order to support incremental releases of changed child modules. This includes automatic discovery of changed modules based on SCM log as well as setting new versions and building only changed POMs and their transitive dependents.

This plugin is still work in progress and not ready for general use.

License
-------
This code is under the [Apache Licence v2][license]

[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://travis-ci.org/stevancvetkovic/incremental-maven-release-plugin
