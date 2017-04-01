package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.vivid.VividAstInspector
import spock.lang.Specification
import spock.lang.Subject

class VividAstInspectorSpec extends Specification {

    @Subject
    final inspector = new VividAstInspector()

    def "Vivid AST Inspector can load single line code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    given: 'the given block'
        |    def x = 10
        |
        |    when: 'an action is taken'
        |    x += 10
        |   
        |    then: 'the result is right'
        |    x == 20 
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def given = result.getLines( 'my feature', 0 )
        given == [ 'def x = 10' ]

        def when = result.getLines( 'my feature', 1 )
        when == [ 'x += 10' ]

        def then = result.getLines( 'my feature', 2 )
        then == [ 'x == 20' ]
    }

    def "Vivid AST Inspector can load empty code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "A first test with Then code block"() {
        |    given:
        |    "we have x and y"
        |
        |    and:
        |    "some more things"
        |
        |    when:
        |    "I do crazy things"
        |
        |    and:
        |
        |    then:
        |    2 == 2
        |  }
        |}
        |'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def given = result.getLines( 'A first test with Then code block', 0 )
        given.isEmpty()

        def given2 = result.getLines( 'A first test with Then code block', 1 )
        given2.isEmpty()

        def when = result.getLines( 'A first test with Then code block', 2 )
        when.isEmpty()

        // the "and" block does not have a label or statements, so it's completely ignored

        def then = result.getLines( 'A first test with Then code block', 3 )
        then == [ '2 == 2' ]
    }

    def "Vivid AST Inspector can load multi-line code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    when:
        |    def x = 10
        |    def y = 20
        |    def z = x + y
        |
        |    then: 'the result is right'
        |    x + y == z
        |    assert x * 2 == y
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def when = result.getLines( 'my feature', 0 )
        when == [ 'def x = 10', 'def y = 20', 'def z = x + y' ]

        def then = result.getLines( 'my feature', 1 )
        then == [ 'x + y == z', 'assert x * 2 == y' ]
    }

    def "Vivid AST Inspector can load multi-line expressions from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    when:
        |    def x = 10 +
        |      20 +
        |      30
        |
        |    and:
        |    def y = """
        |      hello
        |      world
        |    """
        |
        |    then: 'the result is right'
        |    x ==
        |    60
        |
        |    and:
        |    y == 'hello world'
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def when = result.getLines( 'my feature', 0 )
        when == [ 'def x = 10 +\n20 +\n30' ]

        def when2 = result.getLines( 'my feature', 1 )
        when2 == [ 'def y = """\n' +
                           '  hello\n' +
                           '  world\n' +
                           '"""' ]

        def then = result.getLines( 'my feature', 2 )
        then == [ 'x ==\n60' ]

        def then2 = result.getLines( 'my feature', 3 )
        then2 == [ "y == 'hello world'" ]
    }

    def "Vivid AST Inspector does not load the where and cleanup code blocks from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class SomeSpec extends Specification {
        |  def "examples feature"() {
        |    expect:
        |    x < y
        |
        |    where: 'the examples are'
        |    x   | y
        |    20  | 30
        |    100 | 2000
        |
        |    cleanup: 'Just pretend to cleanup'
        |    x = null
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'SomeSpec' )

        then: 'The inspector should be able to provide the source code for each block'
        def then = result.getLines( 'examples feature', 0 )
        then == [ 'x < y' ]

        and: 'The where block is not captured'
        def where = result.getLines( 'examples feature', 1 )
        where.isEmpty()

        and: 'The cleanup block is not captured'
        def cleanup = result.getLines( 'examples feature', 2 )
        cleanup.isEmpty()
    }

    def "Vivid AST Inspector captures all nested statements from Groovy Specification files"() {
        given: 'A Groovy source file'
        def groovySource = '''|
        |class Abc extends Specification {
        |  def "my feature"() {
        |    given: 'the given block'
        |    def x = []
        |    for (i in 0..10) {
        |      x << i
        |      x << i * 2
        |    }
        |    int i = 0
        |    while (i < 4) {
        |      for (j in 0..i) {
        |        x << [i, j]
        |      }
        |      println x
        |    }
        |
        |    when: 'an action is taken'
        |    def y = x.collect { item ->
        |      (0..item).filter { i ->
        |        if (i > 4) {
        |          true
        |        } else false
        |      }
        |    }
        |
        |    then: 'the result is right'
        |    x.collect { i ->
        |      i as String
        |    } == [ '1', '2' ]
        |  }
        |}'''.stripMargin()

        def groovyFile = File.createTempFile( 'spock-reports', 'groovy' )
        groovyFile << groovySource

        when: 'The Groovy file is loaded by the inspector'
        def result = inspector.load( groovyFile, 'Abc' )

        then: 'The inspector should be able to provide the source code for each block'
        def given = result.getLines( 'my feature', 0 )
        given == [ 'def x = []',
                   'for (i in 0..10) {\n  x << i\n  x << i * 2\n}',
                   'int i = 0',
                   'while (i < 4) {\n  for (j in 0..i) {\n    x << [i, j]\n  }\n  println x\n}' ]

        def when = result.getLines( 'my feature', 1 )
        when == [ 'def y = x.collect { item ->\n' +
                          '  (0..item).filter { i ->\n' +
                          '    if (i > 4) {\n' +
                          '      true\n' +
                          '    } else false\n' +
                          '  }\n' +
                          '}' ]

        def then = result.getLines( 'my feature', 2 )
        then == [ 'x.collect { i ->\n' +
                          '  i as String\n' +
                          "} == [ '1', '2' ]" ]
    }

}
