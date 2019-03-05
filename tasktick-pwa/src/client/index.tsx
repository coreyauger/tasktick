import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { Provider } from 'mobx-react';
import { MuiThemeProvider, createMuiTheme } from '@material-ui/core/styles';


import createBrowserHistory from 'history/createBrowserHistory';
import { syncHistoryWithStore } from 'mobx-react-router';
import { Router, Route } from 'react-router';

import App from './App';

const browserHistory = createBrowserHistory();

import stores from './stores/index';

const history = syncHistoryWithStore(browserHistory, stores.routing);

ReactDOM.render(
    <Provider {...stores}>
        <Router history={history}>
            <App store={stores} />           
        </Router>
    </Provider>,
    document.getElementById('root')
);
