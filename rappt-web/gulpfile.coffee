config = require "./gulpconfig"
gulp = require "gulp"
args = require("yargs").argv
gulpif = require "gulp-if"
gutil = require "gulp-util"
coffee = require "gulp-coffee"
coffeelint = require "gulp-coffeelint"
concat = require "gulp-concat"
ngmin = require "gulp-ngmin"
uglify = require "gulp-uglify"
minifyCSS = require "gulp-minify-css"
less = require "gulp-less"
clean = require "gulp-clean"
templateCache = require "gulp-angular-templatecache"
inject = require "gulp-inject"
rev = require "gulp-rev"
size = require "gulp-size"
watch = require "gulp-watch"
queue = require "streamqueue"
runSequence = require "run-sequence"
filter = require "gulp-filter"
plumber = require "gulp-plumber"
path = require "path"
through = require "through"
livereload = require "gulp-livereload"
express = require "express"
server = require("http").createServer express().use(express.static config.buildDir)

# Display info regarding the selected build environment.
throw new Error "No environments configured." if config.environments.length is 0
env = if args.env? then (e for e in config.environments when e.name is args.env)[0] else config.environments[0]
throw new Error "Unsupported environment specified." unless env?
gutil.log gutil.colors.gray "Environment = #{env.name}."
gutil.log gutil.colors.gray "To select other environments, execute \"gulp --env <environment>\"."
gutil.log gutil.colors.gray "Available environments: #{e.name for e in config.environments}." + gutil.linefeed

logError = (error) ->
  gutil.log gutil.colors.red error.message
  gutil.beep()

gulp.task "clean-css", ->
  gulp
    .src "#{config.buildDir}/**/*.css", read: no
    .pipe clean()

gulp.task "build-css", ["clean-css"], ->
  toLessImportStatement = -> through (file) ->
    filePath = file.path.substring file.cwd.length + 1
    file.contents = new Buffer switch path.extname filePath
      when ".less" then "@import (less) '#{filePath}';"
      when ".css" then "@import (inline) '#{filePath}';"
    this.push file

  gulp.src config.styles.concat(env.styles)
    .pipe plumber logError
    .pipe toLessImportStatement()
    .pipe concat "styles.less"
    .pipe less()
    .pipe gulpif env.minify, minifyCSS()
    .pipe rev()
    .pipe size showFiles: yes
    .pipe gulp.dest config.buildDir

gulp.task "clean-js", ->
  gulp
    .src "#{config.buildDir}/**/*.js", read: no
    .pipe clean()

gulp.task "build-js", ["clean-js"], ->
  coffeeFilter = filter "**/*.coffee"

  jsFiles = gulp
    .src config.scripts.concat(env.scripts)
    .pipe plumber logError
    .pipe coffeeFilter
    .pipe coffeelint
      arrow_spacing: level: "error"
      max_line_length: level: "ignore"
    .pipe coffeelint.reporter()
    .pipe coffee()
    .pipe coffeeFilter.restore()

  templateFiles = gulp
    .src config.templates
    .pipe templateCache standalone: yes

  queue objectMode: yes, jsFiles, templateFiles # Queue guarantees concat order!
    .pipe concat "scripts.js"
    .pipe gulpif env.minify, ngmin()
    .pipe gulpif env.minify, uglify mangle: no
    .pipe rev()
    .pipe size showFiles: yes
    .pipe gulp.dest config.buildDir

gulp.task "clean-index", ->
  gulp
    .src "#{config.buildDir}/rappt-app.html", read: no
    .pipe clean()

gulp.task "build-index", ["clean-index"], ->
  gulp
    .src config.index
    .pipe inject(
      gulp.src ["#{config.buildDir}/**/*.css", "#{config.buildDir}/**/*.js"]
      ignorePath: config.buildDir
      read: no
      addRootSlash: no
    )
    .pipe size showFiles: yes
    .pipe gulp.dest config.buildDir

gulp.task "clean-public", ->
  gulp
    .src "#{config.buildDir}/public/**/*", read: no
    .pipe clean()

gulp.task "build-fonts", ["clean-public"], ->
  gulp
    .src config.fonts
    .pipe size showFiles: yes
    .pipe gulp.dest "#{config.buildDir}/public/fonts"

gulp.task "build-images", ["clean-public"], ->
  gulp
    .src config.images
    .pipe size showFiles: yes
    .pipe gulp.dest "#{config.buildDir}/public/images"

gulp.task "build-public", ["clean-public", "build-fonts", "build-images"], ->
  gulp
    .src config.public
    .pipe size showFiles: yes
    .pipe gulp.dest "#{config.buildDir}/public"

gulp.task "build", (done) ->
  # Build everything.
  runSequence ["build-css", "build-js", "build-public"], ["build-index"], done

gulp.task "server", ["build"], (done) ->
  server.listen config.serverPort, ->
    gutil.log gutil.colors.green "Server started and listening on port #{server.address().port}."
    done()

gulp.task "livereload", ->
  gulp
    .src config.buildDir
    .pipe livereload()

gulp.task "watch", ["server", "livereload"], ->
  # When a file changes, only re-build that specific "part" of the app.
  watch glob: config.styles.concat(env.styles), emitOnGlob: no,
    (events, done) -> runSequence ["build-css"], ["build-index"], ["livereload"], done

  watch glob: config.scripts.concat(env.scripts, config.templates), emitOnGlob: no,
    (events, done) -> runSequence ["build-js"], ["build-index"], ["livereload"], done

  watch glob: config.index, emitOnGlob: no,
    (events, done) -> runSequence ["build-index"], ["livereload"], done

  watch glob: config.public, emitOnGlob: no,
    (events, done) -> runSequence ["build-public"], ["livereload"], done

gulp.task "default", ->
  # The default task (i.e. "gulp" via the CLI).
  gulp.start "build"
