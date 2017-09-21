// Specify basic variables to be used in the DSL
String name = 'dnsimple-cookbook'
String repourl = 'https://github.com/dnsimple/chef-dnsimple.git'

def kitchenFile = readFileFromWorkspace('tk/dnsimple.yml')

freeStyleJob(name) {

    scm {
        git {
            remote {
                url(repourl)
            }
            branch('master')
        }
    }

    steps {
        shell readFileFromWorkspace('resources/check_for_md_files.sh')
        shell sprintf('#!/bin/bash\ncat << EOF > .kitchen.azure.yml\n%s\nEOF', kitchenFile)
        shell '''
            #!/bin/bash
            chef exec gem install kitchen-azurerm -N
            KITCHEN_YAML=".kitchen.azure.yml" chef exec rake all
        '''.stripIndent().trim()
    }

    wrappers {
        colorizeOutput()
        preBuildCleanup()
        credentialsBinding {
            string("AZURE_CLIENT_ID", "AZURE_CLIENT_ID")
            string("AZURE_CLIENT_SECRET", "AZURE_CLIENT_SECRET")
            string("AZURE_TENANT_ID", "AZURE_TENANT_ID")
        }
    }

    triggers {
        cron('@midnight')
    }
    
    publishers {
        task('Class: Kitchen::ActionFailed', readFileFromWorkspace('resources/chef_exec_kitchen_destroy.sh'))
    }
}