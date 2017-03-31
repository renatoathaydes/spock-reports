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

}
