ImageJ 1.x plugin to open [DEN files](https://kulvait.github.io/KCT_doc/den-format.html)

How to use this plugin
======================

Clone the repository

* `git clone https://github.com/kulvait/KCT_den_file_opener.git`

Then:

Install maven by `apt-get install maven`
Run `mvn` command
Copy jar file from target directory to ~/.imagej/plugins 

## Documentation

For usage examples go to [KCT framework wiki](https://kulvait.github.io/KCT_doc/)

## Drag and drop

I have developped a simple drag and drop via providing custom HandleExtraFileTypes.

It might conflict with other plugins providing the same functionality, namely in recent Fiji update it conflicted with IO plugin. As the plugin priority is by name alphabetically, you can rename the jar file to something starting with A to have it loaded first or rename IO plugin to something starting with Z to have it loaded last.

To find conflicting plugins just search for "Find Jar For Class" and then type "HandleExtraFileTypes" to see all plugins providing this functionality. There might be other ways how to provide drag and drop functionality, e.g. via SCIFIO, but I have not explored them yet, see [this discussion](https://imagej.nih.narkive.com/QUoWdvgX/handle-extra-files-types-not-working).

## Licensing

When there is no other licensing and/or copyright information in the source files of this project, the following apply for the source files:

Copyright (C) 2018-2025 Vojtěch Kulvait

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, version 3 of the License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
